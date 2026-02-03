const util = require("@openremote/util");
const {rspack} = require("@rspack/core");
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const packageJson = require('./package.json');
const rawLoader = require('raw-loader');

module.exports = (env, argv) => {

    const customConfigDir = env.config;
    const managerUrl = env.managerUrl;
    const keycloakUrl = env.keycloakUrl;
    const IS_DEV_SERVER = process.argv.find(arg => arg.includes("serve"));
    const config = util.getAppConfig(argv.mode, IS_DEV_SERVER, __dirname, managerUrl, keycloakUrl);

    if (IS_DEV_SERVER && customConfigDir) {
        console.log("CUSTOM_CONFIG_DIR: " + customConfigDir);
        // Try and include the static files in the specified config dir if we're in dev server mode
        config.plugins.push(new rspack.CopyRspackPlugin({
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

    config.module.rules.push({
        test: /\.md$/i,
        use: 'raw-loader'
    });

    config.plugins.push(new rspack.CopyRspackPlugin({
        patterns: [
            { from: 'locales', to: 'locales' },
            { from: 'fonts', to: 'fonts' }
        ]
    }));

    // Add a custom base URL to resolve the config dir to the path of the dev server not root
    config.plugins.push(
        new rspack.DefinePlugin({
            CONFIG_URL_PREFIX: JSON.stringify(IS_DEV_SERVER && customConfigDir ? "/ourgrid" : ""),
            OURGRID_APP_VERSION: JSON.stringify(packageJson.version)
        })
    );

    return config;
};
