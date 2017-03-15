drop table KaleoDefinitionVersion;

create table KaleoDefinitionVersion (
	kaleoDefinitionVersionId LONG not null primary key,
	groupId LONG,
	companyId LONG,
	userId LONG,
	userName VARCHAR(200) null,
	statusByUserId LONG,
	statusByUserName VARCHAR(75) null,
	statusDate DATE null,
	createDate DATE null,
	kaleoDefinitionId LONG,
	name VARCHAR(200) null,
	title STRING null,
	description STRING null,
	content TEXT null,
	version VARCHAR(75) null,
	active_ BOOLEAN,
	startKaleoNodeId LONG,
	status INTEGER
);

create index IX_4E8F25EC on KaleoDefinitionVersion (companyId, active_);
create index IX_6AEF26CD on KaleoDefinitionVersion (companyId, name[$COLUMN_LENGTH:200$], active_);
create unique index IX_AE02DCC on KaleoDefinitionVersion (companyId, name[$COLUMN_LENGTH:200$], version[$COLUMN_LENGTH:75$]);
create unique index IX_F0F0CDB5 on KaleoDefinitionVersion (kaleoDefinitionId, version[$COLUMN_LENGTH:75$]);

COMMIT_TRANSACTION;