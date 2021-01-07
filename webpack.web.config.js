const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyPlugin = require('copy-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const { DefinePlugin } = require('webpack');

const config = {
    node: {
        fs: "empty"
    },
    
    optimization: {
        usedExports: false,
        splitChunks: {
            cacheGroups: {
                default: false,
                vendors: false,
                vendor: {
                    // sync + async chunks
                    chunks: 'all',
                    // import file path containing node_modules
                    test: /node_modules/,
                    name(module){
                        // get the name. E.g. node_modules/packageName/not/this/part.js
                        // or node_modules/packageName
                        const packageName = module.context.match(/[\\/]node_modules[\\/](.*?)([\\/]|$)/)[1];
                        
                        // npm package names are URL-safe, but some servers don't like @ symbols
                        return `vendor.${packageName.replace('@', '')}`;
                    },
                    reuseExistingChunk: true,
                    minSize: 10000,
                    maxSize: 25000,
                    priority: 20,
                },
                // common chunk
                common: {
                    chunks: 'all',
                    name: 'md',
                    minChunks: 1,
                    priority: 10,
                    reuseExistingChunk: true,
                    enforce: true,
                    maxSize: 100000,
                    minSize: 30000
                }
            },
        }
    },

    devtool: 'source-map',

    devServer: {
        open: 'chrome',
        contentBase: path.join(__dirname, 'dist'),
        compress: true,
        port: 3006,
        watchContentBase: true, 
        watchOptions: {
            poll: true
        },
        historyApiFallback: true, 
        historyApiFallback: {
            index: '/index.html',
        },
        clientLogLevel: 'none',
        watchOptions: {
            poll: 5000,
            ignored: ["node_modules", "relase", "dist"]
        },
        stats: {
            colors: true,
            hash: false,
            version: false,
            timings: false,
            assets: false,
            chunks: false,
            modules: false,
            reasons: false,
            children: false,
            source: false,
            errors: true,
            errorDetails: false,
            warnings: false,
            publicPath: false
        }
    },

    entry: {
        main: './src/index.js',
        shared: ['react', 'react-dom', 'redux', 'react-redux', 'react-table', 'react-bootstrap'],
        diff: './src/common/routes/diff_results/index.js',
        apiCatalog: './src/common/routes/api_catalog/index.js',
        httpClient: './src/common/routes/http_client/index.js',
        testResults: './src/common/routes/test_results/index.js',
        testReport: './src/common/routes/test_report/index.js',
    },

    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: '[name].[contenthash].js',
        chunkFilename: '[id].[contenthash].js',
        publicPath:'/'
    },

    module: {
        rules: [
            {
                test: /\.(js|jsx|ts|tsx)$/,
                exclude: /node_modules/,
                use: {
                    loader: "babel-loader"
                }
            },
            {
                test: /\.(css)$/i,
                use: [
                    // {
                    //     loader: MiniCssExtractPlugin.loader,
                    // },
                    'style-loader',
                    'css-loader',
                ]
            },
            {
                test: /\.(s[ac]ss)$/i,
                use: [
                    {
                        loader: MiniCssExtractPlugin.loader,
                    },
                    // Creates `style` nodes from JS strings
                    // 'style-loader',
                    // Translates CSS into CommonJS
                    'css-loader',
                    // Compiles Sass to CSS
                    'sass-loader',
                ],
            },
            {
                test: /\.html$/,
                use: [
                    {
                        loader: "html-loader"
                    }
                ]
            },
            {
                test: /\.(png|jpe?g|gif|ico|icns)$/i,
                use: [
                    {
                        loader: 'file-loader',
                    },
                ],
            },
            {
                test: /\.(woff|woff2|eot|ttf|otf)$/,
                use: [
                    {
                        loader: 'file-loader',
                    },
                ],
            },
            {
                test: /\.svg$/,
                loader: 'svg-inline-loader'
            }
        ]
    },

    plugins: [
        new CleanWebpackPlugin(),
        new DefinePlugin({
            PLATFORM_ELECTRON: false,
        }),
        new CopyPlugin({
            patterns: [
                { from: path.resolve(__dirname, 'public'), to: '' }
            ],
        }),
        new MiniCssExtractPlugin({
            filename: '[name].css',
            chunkFilename: '[id].css',
        }),
        new HtmlWebpackPlugin({
            title: 'Mesh Dynamics',
            template: './src/templates/index.html',
            filename: 'index.html'
        })
    ],
    resolve:{
        extensions: ['.ts', '.js', '.tsx']
    }

};


module.exports = config;

// globOptions: {
//     ignore: [
//         'index.html',
//         './pages/settings.html'
//     ],
// },