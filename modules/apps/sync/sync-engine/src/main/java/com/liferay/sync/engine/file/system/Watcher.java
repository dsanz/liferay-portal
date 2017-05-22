/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.sync.engine.file.system;

import com.liferay.sync.engine.file.system.util.WatcherManager;
import com.liferay.sync.engine.model.SyncAccount;
import com.liferay.sync.engine.model.SyncFile;
import com.liferay.sync.engine.model.SyncSite;
import com.liferay.sync.engine.model.SyncWatchEvent;
import com.liferay.sync.engine.service.SyncAccountService;
import com.liferay.sync.engine.service.SyncFileService;
import com.liferay.sync.engine.service.SyncSiteService;
import com.liferay.sync.engine.service.SyncWatchEventService;
import com.liferay.sync.engine.util.FileKeyUtil;
import com.liferay.sync.engine.util.FileUtil;
import com.liferay.sync.engine.util.MSOfficeFileUtil;
import com.liferay.sync.engine.util.OSDetector;

import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Young
 * @author Shinn Lok
 */
public abstract class Watcher implements Runnable {

	public Watcher(long syncAccountId, Path filePath) {
		_syncAccountId = syncAccountId;
		_baseFilePath = filePath;

		init();
	}

	public void addDeletedFilePathName(String filePathName) {
		_deletedFilePathNames.add(filePathName);
	}

	public void addDownloadedFilePathName(String filePathName) {
		_downloadedFilePathNames.add(filePathName);
	}

	public void addMovedFilePathName(String filePathName) {
		_movedFilePathNames.add(filePathName);
	}

	public void close() {
		WatcherManager.removeWatcher(_syncAccountId);
	}

	public abstract void registerFilePath(Path filePath) throws IOException;

	public void removeDeletedFilePathName(String filePathName) {
		_deletedFilePathNames.remove(filePathName);
	}

	public void removeDownloadedFilePathName(String filePathName) {
		_downloadedFilePathNames.remove(filePathName);
	}

	public void removeMovedFilePathName(String filePathName) {
		_movedFilePathNames.remove(filePathName);
	}

	public abstract void unregisterFilePath(Path filePath);

	public void walkFileTree(final Path rootFilePath, final boolean skipRoot)
		throws IOException {

		if (isIgnoredFilePath(rootFilePath)) {
			return;
		}

		Files.walkFileTree(
			rootFilePath,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult postVisitDirectory(
						Path filePath, IOException ioe)
					throws IOException {

					if (ioe != null) {
						_failedFilePaths.add(filePath);

						return FileVisitResult.CONTINUE;
					}

					return super.postVisitDirectory(filePath, null);
				}

				@Override
				public FileVisitResult preVisitDirectory(
						Path filePath, BasicFileAttributes basicFileAttributes)
					throws IOException {

					if (skipRoot && rootFilePath.equals(filePath)) {
						return FileVisitResult.CONTINUE;
					}

					if (filePath.equals(_baseFilePath.resolve(".data")) ||
						isIgnoredFilePath(filePath)) {

						return FileVisitResult.SKIP_SUBTREE;
					}

					SyncFile syncFile = SyncFileService.fetchSyncFile(
						filePath.toString());

					if (syncFile == null) {
						fireWatchEventListener(
							SyncWatchEvent.EVENT_TYPE_CREATE, filePath);
					}

					try {
						registerFilePath(filePath);
					}
					catch (IOException ioe) {
						_logger.error(ioe.getMessage(), ioe);
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(
						Path filePath, BasicFileAttributes basicFileAttributes)
					throws IOException {

					if (isIgnoredFilePath(filePath)) {
						return FileVisitResult.CONTINUE;
					}

					SyncFile syncFile = SyncFileService.fetchSyncFile(
						filePath.toString());

					if (syncFile == null) {
						fireWatchEventListener(
							SyncWatchEvent.EVENT_TYPE_CREATE, filePath);
					}
					else if (FileUtil.isModified(syncFile, filePath)) {
						fireWatchEventListener(
							SyncWatchEvent.EVENT_TYPE_MODIFY, filePath);
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(
						Path filePath, IOException ioe)
					throws IOException {

					_failedFilePaths.add(filePath);

					return FileVisitResult.CONTINUE;
				}

			});
	}

	public void watchEvent(String eventType, Path filePath) {
		addSyncWatchEvent(eventType, filePath);
	}

	protected void addCreatedFilePathName(String filePathName) {
		clearCreatedFilePathNames();

		long now = System.currentTimeMillis();

		while (_createdFilePathNames.putIfAbsent(now, filePathName) != null) {
			now++;
		}
	}

	protected synchronized void addSyncWatchEvent(
		String eventType, Path filePath) {

		try {
			String filePathName = filePath.toString();

			if (isDuplicateEvent(eventType, filePathName, _syncAccountId)) {
				return;
			}

			SyncAccount syncAccount = SyncAccountService.fetchSyncAccount(
				_syncAccountId);

			if (filePathName.equals(syncAccount.getFilePathName())) {
				return;
			}

			Path parentFilePath = filePath.getParent();

			String parentFilePathName = parentFilePath.toString();

			if (parentFilePathName.equals(syncAccount.getFilePathName())) {
				SyncSite syncSite = SyncSiteService.fetchSyncSite(
					filePathName, _syncAccountId);

				if ((syncSite == null) || syncSite.isActive()) {
					return;
				}

				SyncFile syncFile = SyncFileService.fetchSyncFile(filePathName);

				if (FileKeyUtil.hasFileKey(
						filePath, syncFile.getSyncFileId())) {

					if (_logger.isDebugEnabled()) {
						_logger.debug(
							"Sync site {} reactivated.", syncSite.getName());
					}

					SyncSiteService.activateSyncSite(
						syncSite.getSyncSiteId(),
						Collections.<SyncFile>emptyList(), false);
				}

				return;
			}

			long repositoryId = getRepositoryId(filePath);

			if (repositoryId <= 0) {
				return;
			}

			SyncSite syncSite = SyncSiteService.fetchSyncSite(
				repositoryId, _syncAccountId);

			if (!syncSite.isActive()) {
				return;
			}

			if (!eventType.equals(SyncWatchEvent.EVENT_TYPE_RENAME_TO)) {
				if (_deletedFilePathNames.remove(filePath.toString()) ||
					_downloadedFilePathNames.remove(filePath.toString())) {

					return;
				}

				SyncWatchEventService.addSyncWatchEvent(
					eventType, filePathName, getFileType(eventType, filePath),
					null, _syncAccountId);

				return;
			}

			String previousEventType = null;
			Path previousFilePath = null;
			long previousRepositoryId = 0;

			SyncWatchEvent lastSyncWatchEvent =
				SyncWatchEventService.getLastSyncWatchEvent(_syncAccountId);

			if (lastSyncWatchEvent != null) {
				previousEventType = lastSyncWatchEvent.getEventType();

				previousFilePath = Paths.get(
					lastSyncWatchEvent.getFilePathName());

				previousRepositoryId = getRepositoryId(previousFilePath);
			}

			String fileType = getFileType(eventType, filePath);

			if ((lastSyncWatchEvent == null) ||
				!previousEventType.equals(
					SyncWatchEvent.EVENT_TYPE_RENAME_FROM) ||
				(previousRepositoryId != repositoryId)) {

				eventType = SyncWatchEvent.EVENT_TYPE_CREATE;

				if (_downloadedFilePathNames.remove(filePath.toString())) {
					return;
				}

				SyncWatchEventService.addSyncWatchEvent(
					eventType, filePathName, getFileType(eventType, filePath),
					null, _syncAccountId);

				if (fileType.equals(SyncFile.TYPE_FOLDER)) {
					SyncFile syncFile = SyncFileService.fetchSyncFile(
						filePathName);

					if (syncFile != null) {
						FileUtil.fireDeleteEvents(Paths.get(filePathName));
					}

					Watcher watcher = WatcherManager.getWatcher(_syncAccountId);

					watcher.walkFileTree(Paths.get(filePathName), true);
				}
			}
			else if (filePath.equals(previousFilePath)) {
				lastSyncWatchEvent.setEventType(
					SyncWatchEvent.EVENT_TYPE_MODIFY);

				SyncWatchEventService.update(lastSyncWatchEvent);
			}
			else if (parentFilePath.equals(previousFilePath.getParent())) {
				if (MSOfficeFileUtil.isTempRenamedFile(
						previousFilePath, filePath)) {

					SyncWatchEventService.deleteSyncWatchEvent(
						lastSyncWatchEvent.getSyncWatchEventId());

					return;
				}

				if (_movedFilePathNames.remove(filePath.toString())) {
					return;
				}

				lastSyncWatchEvent.setEventType(
					SyncWatchEvent.EVENT_TYPE_RENAME);
				lastSyncWatchEvent.setFilePathName(filePathName);
				lastSyncWatchEvent.setFileType(fileType);
				lastSyncWatchEvent.setPreviousFilePathName(
					previousFilePath.toString());

				SyncWatchEventService.update(lastSyncWatchEvent);

				if (fileType.equals(SyncFile.TYPE_FOLDER)) {
					Watcher watcher = WatcherManager.getWatcher(_syncAccountId);

					watcher.walkFileTree(Paths.get(filePathName), true);
				}
			}
			else {
				SyncFile syncFile = SyncFileService.fetchSyncFile(filePathName);

				if ((syncFile != null) &&
					fileType.equals(SyncFile.TYPE_FOLDER)) {

					FileUtil.fireDeleteEvents(Paths.get(filePathName));

					Watcher watcher = WatcherManager.getWatcher(_syncAccountId);

					watcher.walkFileTree(Paths.get(filePathName), true);

					watchEvent(SyncWatchEvent.EVENT_TYPE_DELETE, filePath);
				}
				else {
					if (_movedFilePathNames.remove(filePath.toString())) {
						return;
					}

					lastSyncWatchEvent.setEventType(
						SyncWatchEvent.EVENT_TYPE_MOVE);
					lastSyncWatchEvent.setFilePathName(filePathName);
					lastSyncWatchEvent.setPreviousFilePathName(
						previousFilePath.toString());

					SyncWatchEventService.update(lastSyncWatchEvent);

					if (fileType.equals(SyncFile.TYPE_FOLDER)) {
						Watcher watcher = WatcherManager.getWatcher(
							_syncAccountId);

						watcher.walkFileTree(Paths.get(filePathName), true);
					}
				}
			}
		}
		catch (Exception e) {
			_logger.error(e.getMessage(), e);
		}
	}

	protected void clearCreatedFilePathNames() {
		Map<Long, String> map = _createdFilePathNames.headMap(
			System.currentTimeMillis() - 5000);

		map.clear();
	}

	protected void fireWatchEventListener(String eventType, Path filePath) {
		watchEvent(eventType, filePath);
	}

	protected Path getBaseFilePath() {
		return _baseFilePath;
	}

	protected List<Path> getFailedFilePaths() {
		return _failedFilePaths;
	}

	protected String getFileType(String eventType, Path filePath) {
		if (eventType.equals(SyncWatchEvent.EVENT_TYPE_DELETE) ||
			eventType.equals(SyncWatchEvent.EVENT_TYPE_RENAME_FROM)) {

			SyncFile syncFile = SyncFileService.fetchSyncFile(
				filePath.toString());

			if (syncFile != null) {
				return syncFile.getType();
			}
		}

		if (Files.isDirectory(filePath, LinkOption.NOFOLLOW_LINKS)) {
			return SyncFile.TYPE_FOLDER;
		}

		return SyncFile.TYPE_FILE;
	}

	protected long getRepositoryId(Path filePath) {
		while (true) {
			filePath = filePath.getParent();

			if (filePath == null) {
				return 0;
			}

			SyncFile syncFile = SyncFileService.fetchSyncFile(
				filePath.toString());

			if (syncFile != null) {
				return syncFile.getRepositoryId();
			}
		}
	}

	protected abstract void init();

	protected boolean isDuplicateEvent(
		String eventType, String filePathName, long syncAccountId) {

		SyncWatchEvent lastSyncWatchEvent =
			SyncWatchEventService.getLastSyncWatchEvent(syncAccountId);

		if ((lastSyncWatchEvent == null) ||
			!filePathName.equals(lastSyncWatchEvent.getFilePathName()) ||
			!eventType.equals(lastSyncWatchEvent.getEventType())) {

			return false;
		}

		return true;
	}

	protected boolean isIgnoredFilePath(Path filePath) {
		if (FileUtil.notExists(filePath)) {
			return true;
		}

		String fileName = String.valueOf(filePath.getFileName());

		if (FileUtil.isIgnoredFilePath(filePath) ||
			FileUtil.isUnsynced(filePath) || (fileName.length() > 255)) {

			if (_logger.isDebugEnabled()) {
				_logger.debug("Ignored file path {}", filePath);
			}

			return true;
		}

		return false;
	}

	protected void processFailedFilePaths() throws IOException {
		List<Path> failedFilePaths = getFailedFilePaths();

		for (Path failedFilePath : failedFilePaths) {
			if (FileUtil.notExists(failedFilePath)) {
				failedFilePaths.remove(failedFilePath);

				continue;
			}

			if (!Files.isReadable(failedFilePath)) {
				continue;
			}

			failedFilePaths.remove(failedFilePath);

			if (Files.isDirectory(failedFilePath)) {
				registerFilePath(failedFilePath);
			}

			SyncFile syncFile = SyncFileService.fetchSyncFile(
				failedFilePath.toString());

			if (syncFile == null) {
				fireWatchEventListener(
					SyncWatchEvent.EVENT_TYPE_CREATE, failedFilePath);
			}
			else if (FileUtil.isModified(syncFile, failedFilePath)) {
				fireWatchEventListener(
					SyncWatchEvent.EVENT_TYPE_MODIFY, failedFilePath);
			}
		}
	}

	protected void processMissingFilePath(Path missingFilePath) {
		SyncAccount syncAccount = SyncAccountService.fetchSyncAccount(
			_syncAccountId);

		Path syncAccountFilePath = Paths.get(syncAccount.getFilePathName());

		if (!FileUtil.isRealFilePath(syncAccountFilePath) ||
			!FileUtil.exists(syncAccountFilePath)) {

			if (_logger.isTraceEnabled()) {
				_logger.trace(
					"Missing sync account file path {}", syncAccountFilePath);
			}

			syncAccount.setActive(false);
			syncAccount.setUiEvent(
				SyncAccount.UI_EVENT_SYNC_ACCOUNT_FOLDER_MISSING);

			SyncAccountService.update(syncAccount);
		}
		else {
			SyncSite syncSite = SyncSiteService.fetchSyncSite(
				missingFilePath.toString(), syncAccount.getSyncAccountId());

			if ((syncSite != null) && syncSite.isActive() &&
				(!FileUtil.isRealFilePath(syncAccountFilePath) ||
				 !FileUtil.exists(missingFilePath))) {

				if (_logger.isTraceEnabled()) {
					_logger.trace(
						"Missing sync site file path {}", missingFilePath);
				}

				syncSite.setActive(false);
				syncSite.setUiEvent(SyncSite.UI_EVENT_SYNC_SITE_FOLDER_MISSING);

				SyncSiteService.update(syncSite);
			}
		}
	}

	protected void processWatchEvent(String eventType, Path filePath)
		throws IOException {

		if (!OSDetector.isLinux() &&
			filePath.startsWith(_baseFilePath.resolve(".data"))) {

			return;
		}

		_watcherEventsLogger.trace("{}: {}", eventType, filePath);

		if (eventType.equals(SyncWatchEvent.EVENT_TYPE_CREATE)) {
			if (isIgnoredFilePath(filePath)) {
				return;
			}

			addCreatedFilePathName(filePath.toString());

			fireWatchEventListener(eventType, filePath);

			if (!OSDetector.isApple() && Files.isDirectory(filePath)) {
				walkFileTree(filePath, false);
			}
		}
		else if (eventType.equals(SyncWatchEvent.EVENT_TYPE_DELETE)) {
			if (FileUtil.exists(filePath)) {
				return;
			}

			removeCreatedFilePathName(filePath.toString());

			if (FileUtil.isIgnoredFileName(
					String.valueOf(filePath.getFileName())) ||
				FileUtil.isTempFile(filePath)) {

				return;
			}

			processMissingFilePath(filePath);

			if (FileUtil.notExists(filePath.getParent())) {
				return;
			}

			fireWatchEventListener(SyncWatchEvent.EVENT_TYPE_DELETE, filePath);
		}
		else if (eventType.equals(SyncWatchEvent.EVENT_TYPE_MODIFY)) {
			if ((removeCreatedFilePathName(filePath.toString()) &&
				 !FileUtil.isValidChecksum(filePath)) ||
				FileUtil.isIgnoredFileName(
					String.valueOf(filePath.getFileName())) ||
				FileUtil.isTempFile(filePath) || FileUtil.notExists(filePath) ||
				Files.isDirectory(filePath) || FileUtil.isHidden(filePath) ||
				FileUtil.isShortcut(filePath)) {

				return;
			}

			fireWatchEventListener(SyncWatchEvent.EVENT_TYPE_MODIFY, filePath);
		}
		else if (eventType.equals(SyncWatchEvent.EVENT_TYPE_RENAME_FROM)) {
			removeCreatedFilePathName(filePath.toString());

			if (FileUtil.isTempFile(filePath)) {
				return;
			}

			processMissingFilePath(filePath);

			fireWatchEventListener(
				SyncWatchEvent.EVENT_TYPE_RENAME_FROM, filePath);
		}
		else if (eventType.equals(SyncWatchEvent.EVENT_TYPE_RENAME_TO)) {
			if (FileUtil.isIgnoredFileName(
					String.valueOf(filePath.getFileName())) ||
				FileUtil.isHidden(filePath) || FileUtil.isShortcut(filePath)) {

				if (_logger.isDebugEnabled()) {
					_logger.debug("Ignored file path {}", filePath);
				}

				return;
			}

			fireWatchEventListener(
				SyncWatchEvent.EVENT_TYPE_RENAME_TO, filePath);
		}
	}

	protected boolean removeCreatedFilePathName(String filePathName) {
		clearCreatedFilePathNames();

		Collection<String> filePathNames = _createdFilePathNames.values();

		return filePathNames.remove(filePathName);
	}

	private static final Logger _logger = LoggerFactory.getLogger(
		Watcher.class);

	private static final Logger _watcherEventsLogger = LoggerFactory.getLogger(
		"WATCHER_EVENTS_LOGGER");

	private final Path _baseFilePath;
	private final ConcurrentNavigableMap<Long, String> _createdFilePathNames =
		new ConcurrentSkipListMap<>();
	private final List<String> _deletedFilePathNames =
		new CopyOnWriteArrayList<>();
	private final List<String> _downloadedFilePathNames =
		new CopyOnWriteArrayList<>();
	private final List<Path> _failedFilePaths = new CopyOnWriteArrayList<>();
	private final List<String> _movedFilePathNames =
		new CopyOnWriteArrayList<>();
	private final long _syncAccountId;

}