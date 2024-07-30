const util = require("@openremote/util");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const webpack = require("webpack");
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const packageJson = require('./package.json');

module.exports = (env, argv) => {

    const customConfigDir = env.config;
    const managerUrl = env.managerUrl;
    const keycloakUrl = env.keycloakUrl;
    const IS_DEV_SERVER = process.argv.find(arg => arg.includes("serve"));
    const config = util.getAppConfig(argv.mode, IS_DEV_SERVER, __dirname, managerUrl, keycloakUrl);

    if (IS_DEV_SERVER && customConfigDir) {
        console.log("CUSTOM_CONFIG_DIR: " + customConfigDir);
        // Try and include the static files in the specified config dir if we're in dev server mode
        config.plugins.push(new CopyWebpackPlugin({
            patterns: [
                {
                    from: customConfigDir
                },
            ]
        }));

        console.log("Webpack bundle analyzer URL: http://127.0.0.1:8888")
        config.plugins.push(new BundleAnalyzerPlugin({
            openAnalyzer: false
        }))
    }

    config.plugins.push(new CopyWebpackPlugin({
        patterns: [
            { from: 'locales', to: 'locales' },
            { from: 'fonts', to: 'fonts' }
        ]
    }));

    // Add a custom base URL to resolve the config dir to the path of the dev server not root
    config.plugins.push(
        new webpack.DefinePlugin({
            CONFIG_URL_PREFIX: JSON.stringify(IS_DEV_SERVER && customConfigDir ? "/ourgrid" : ""),
            APP_VERSION: JSON.stringify(packageJson.version)
        })
    );

    return config;
};
