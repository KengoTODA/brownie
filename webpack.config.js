var ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = {
    entry: "./src/main/resources/webroot/main.jsx",
    output: {
        path: "src/main/resources/webroot",
        filename: "bundle.js"
    },
    module: {
      loaders: [
        { test: /\.jsx$/, loader: 'jsx-loader' },
        {
          test: /\.css$/,
          loader: ExtractTextPlugin.extract("style-loader", "css-loader")
        },
        {
          test: /\.(eot|ttf|svg|woff|woff2)$/,
          loader: 'url-loader?limit=200000'
        }
      ]
    },
    resolve: {
      extensions: ['', '.js', '.jsx']
    },
    plugins: [
        new ExtractTextPlugin("bundle.css", {
            allChunks: true
        })
    ]
};
