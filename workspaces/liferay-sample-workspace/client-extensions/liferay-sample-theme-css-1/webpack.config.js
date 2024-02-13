/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const FileManagerPlugin = require('filemanager-webpack-plugin');
const fs = require('fs');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');
const RemoveEmptyScriptsPlugin = require('webpack-remove-empty-scripts');

const buildThemeDir = path.join(__dirname, '/build/buildTheme/');
const sourceCSSDir = path.join(__dirname, "src", "css");


module.exports = {
	context: path.join(__dirname, '/build/buildTheme/css/'),
	entry: {
		clay: './clay.css',
		main: './main.css'
	},
	mode: 'production',
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
									if (url.startsWith("data:image") || url.includes("@theme_image_path@")) {
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
		//assetModuleFilename: '../assets/[name].[contenthash][ext]',
		clean: {
			keep(asset) {
				return !['main.css', 'main_rtl.css', 'clay.css', 'clay_rtl.css'].some((file) => asset.includes(file));
			}
		},
		path: buildThemeDir
		//path: path.resolve(__dirname, 'build/buildTheme/css/')
	},
	plugins: [
		new FileManagerPlugin({
			events: {
				onStart: {
					delete: [path.resolve(__dirname, 'build/buildTheme/assets'), path.resolve(__dirname, 'build/buildTheme/css/') + '/main.?*.css', path.resolve(__dirname, 'build/buildTheme/css/') + '/clay.?*.css',  ],
				}
			},
		}),
		new RemoveEmptyScriptsPlugin(),
		new MiniCssExtractPlugin({
			filename: "[name].[contenthash].css"
		})
	]
};

