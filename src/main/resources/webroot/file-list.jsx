var React = require('react');
var ReactDOM = require('react-dom');
var $ = require('./jquery-2.1.4.min');

'use strict';

/**
 * @param {number} x
 * @return {string}
 */
function numberWithCommas(x) {
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

var File = React.createClass({
  displayName: 'File',
  render: function() {
    var url = 'files/' + this.props.fileId;
    var contentLength = numberWithCommas(this.props.contentLength) + ' bytes';
    var generated = new Date(this.props.generated) + '';
    return (
      <tr data-key="{this.prop.fileId}">
        <td><a href="{url}">{this.props.fileName}</a></td>
        <td style={{textAlign: 'right'}}>{contentLength}</td>
        <td>{generated}</td>
      </tr>
    );
  }
});
var FileList = React.createClass({
  displayName: 'FileList',
  getInitialState: function() {
    return {files: []};
  },
  reload: function() {
    $.ajax('files/', {
      type: 'get',
      cache: false,
    })
    .done(function(data) {
      this.setState({files: data});
    }.bind(this));
  },
  componentDidMount: function() {
    this.reload();
    setInterval(this.reload, this.props.pollInterval);
  },
  render: function() {
    if (this.state.files.length) {
      var fileNodes = this.state.files.map(function(file) {
        return (
          <File
              key={file.fileId}
              fileName={file.fileName}
              contentLength={file.contentLength}
              generated={file.generated}>
          </File>
        );
      });
      return (
        <div>
          <h2>Files</h2>
          <table className="table table-striped table-bordered">
            <thead>
              <tr>
                <th>file name</th>
                <th>size</th>
                <th>created time</th>
              </tr>
            </thead>
            <tbody>
              {fileNodes}
            </tbody>
          </table>
        </div>
      );
    } else {
      return (
        <div>
          <h2>Files</h2>
          <p className="text-info">
            No generated file
          </p>
        </div>
      );
    }
  }
});

module.exports = FileList;
