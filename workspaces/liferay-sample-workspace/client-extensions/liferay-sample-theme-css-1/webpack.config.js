/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const FileManagerPlugin = require('filemanager-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const RemoveEmptyScriptsPlugin = require('webpack-remove-empty-scripts');

const fs = require('fs');
const path = require('path');

const buildDir = path.join(__dirname, 'build', 'buildTheme');
const buildAssetsDir = path.join(buildDir, 'assets');
const buildCSSDir = path.join(buildDir, 'css');

const sourceCSSDir = path.join(__dirname, 'src', 'css');
const urlTokens = ['@base_url@', '@portal_ctx@', '@theme_image_path@']

const DEVELOPMENT = process.env.NODE_ENV === 'development';

module.exports = {
	context: buildCSSDir,
	entry: {
		clay: './clay.css',
		main: './main.css'
	},
	mode: DEVELOPMENT ? 'development' : 'production',
	module: {
		rules: [
			{
				test: /\.css$/i,
				use: [
					{
						loader: MiniCssExtractPlugin.loader,
					},
					{
						loader: "css-loader",
						options: {
							url: {
								filter: (url, resourcePath) => {

									if (url.startsWith("data:image") || urlTokens.some((str) => url.includes(str))) {
										return false;
									}

									if (!fs.existsSync(path.join(sourceCSSDir, url))) {
										return false;
									}

									return true;
								}
							}
						}
					}
				]
			},
			{
				generator: {
					filename: 'assets/[name].[contenthash][ext]'
				},
				test: /\.(png|jpe?g|gif|svg|eot|ttf|woff|woff2)$/i,
				type: "asset/resource",
			},
		],
	},
	output: {
		//clean: {
			//keep: '*.scss'

			//	keep(asset) {
//		return !['main.css', 'main_rtl.css', 'clay.css', 'clay_rtl.css'].some((file) => asset.includes(file));
//	}
		//},
		path: buildCSSDir
	},
	plugins: [
		// new FileManagerPlugin({
		// 	events: {
		// 		onStart: {
		// 			delete: [
		// 				buildAssetsDir,
		// 				path.join(buildCSSDir, 'main.?*.css'),
		// 				path.join(buildCSSDir, 'clay.?*.css'),
		// 			],
		// 		},
		// 		onEnd: {
		// 			delete: [
		// 				path.join(buildCSSDir, 'main.js'),
		// 				path.join(buildCSSDir, 'clay.js')
		// 			],
		// 		}
		// 	},
		// }),
		new MiniCssExtractPlugin({
			filename: "[name].[contenthash].css"
		})
	]
};

