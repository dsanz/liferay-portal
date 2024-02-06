const fs = require('fs');
const path = require('path');
const RemoveEmptyScriptsPlugin = require('webpack-remove-empty-scripts');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

module.exports = {
	context: path.join(__dirname, '/build/buildTheme/css/'),
	entry: {
		main: './main.css',
		clay: './clay.css',
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

									console.log("  url " + url + " → " + path.join(__dirname, "src", "css", url))
									if (!fs.existsSync(path.join(__dirname, "src", "css", url))) {
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
				test: /\.(png|jpe?g|gif|svg|eot|ttf|woff|woff2)$/i,
				type: "asset/resource",
			},
		],
	},
	output: {
		clean: {
			keep(asset) {
				return !['main.css', 'main_rtl.css', 'clay.css', 'clay_rtl.css'].some((file) => asset.includes(file));
			},
			//dry: true, // Log the assets that should be removed instead of deleting them.
		},
		assetModuleFilename: 'assets/[name].[contenthash][ext]',
		path: path.resolve(__dirname, 'build/buildTheme/css/')
		//path: path.resolve(__dirname, 'build/liferay-client-extension-build/static2')
	},
	plugins: [
		new RemoveEmptyScriptsPlugin(),
		new MiniCssExtractPlugin({
			filename: "[name].[contenthash].css"
		})
	]
};

