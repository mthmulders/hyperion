var path = require('path');
var webpack = require('webpack');
var excludedFolders = [/node_modules/];
var devtools = 'source-map';
var minimizeCss = 'minimize';

var ExtractTextPlugin = require('extract-text-webpack-plugin');

var env = process.env.WEBPACK_ENV;

var plugins = [
    new ExtractTextPlugin('style.css')
];

if ('production' == env) {
    console.log('This is a production build');
    // Set NODE_ENV to "production" so the React production lib is used. (No warnings in the dev console.)
    plugins.push(new webpack.DefinePlugin({'process.env': { NODE_ENV: JSON.stringify('production') }}));
    // If errors occur do not "emmit" the build to the build-folder.
    plugins.push(new webpack.NoErrorsPlugin());
    // Remove duplicate code.
    plugins.push(new webpack.optimize.DedupePlugin());
    // Minimize javascript
    plugins.push(new webpack.optimize.UglifyJsPlugin({ minimize: true, compressor: { warnings: false}}));

    // Do not create .map source files
    devtools = '';
} else {
    console.log('This is a development build');
    // Do not minimize CSS
    minimizeCss = '-minimize';
}

module.exports = {
    // Entry point for the application
    entry: {
        javascript: "./src/entry.js",
        html: "./src/index.html"
    },
    // Output the result
    output: {
        path: "./build/",
        filename: "bundle.js"
    },
    // Tell webpack where to find files
    resolve: {
        root: path.join(__dirname, 'src'),
        extensions: ['', '.js', '.css', '.svg', '.html'],
        modulesDirectories: ['node_modules']
    },
    // Create maps so we can see the source for our js files
    devtool: devtools,
    module: {
        preLoaders: [{
            test: /\.js$/,
            loader: 'eslint-loader',
            exclude: excludedFolders
        }],
        // Define loaders
        loaders: [
            // CSS Loader
            { test: /\.css$/, loader: ExtractTextPlugin.extract('style-loader', 'css-loader?module&importLoaders=1&localIdentName=[local]&' + minimizeCss + '&sourceMap!postcss-loader')},
            // JS / EcmaScript 6-7 loader.
            { test: /\.js?$/, loader: 'babel-loader', exclude: excludedFolders, query: { presets:['es2015', 'react'] } },
            // JPG / PNG loader
            { test: /\.(jpg|png)$/, loader: 'url-loader?name=[path][name].[ext]&context=src&limit=1' },
            // HTML loader
            { test: /\.html$/, loader: "url-loader?name=[path][name].[ext]&context=src&limit=1"},
            // SVG Loader
            { test: /\.svg$/, loader: 'url-loader?name=[path][name].[ext]&context=src&limit=1&mimetype=image/svg+xml!svgo-loader?useConfig=svgoConfig'}
        ]
    },
    // Used plugins
    plugins: plugins,
    // CSS loader config
    postcss: function (webpack) {
        return [
            require("postcss-import")({ addDependencyTo: webpack }),
            require("postcss-url")(),
            require("postcss-cssnext")(),
            require("postcss-browser-reporter")(),
            require("postcss-reporter")()
        ];
    },
    // eslint config
    eslint: {
        configFile: './.eslintrc',
        emitError: false,
        emitWarning: true
    },
    // svg-loader config
    svgoConfig: {
        plugins: [
            { removeTitle: true },
            { convertColors: { shorthex: false } },
            { convertPathData: false }
        ]
    }
};