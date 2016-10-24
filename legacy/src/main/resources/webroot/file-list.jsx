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

class File extends React.Component {
  deleteFile() {
    $.ajax('files/' + this.props.id, {
      type: 'delete',
      cache: false
    });
  }

  render() {
    let url = 'files/' + this.props.id;
    let contentLength = numberWithCommas(this.props.content_length) + ' bytes';
    let generated = new Date(this.props.generated) + '';
    return (
      <tr>
        <td>
          <a href={url}>{this.props.name}</a>
          <i className="deleteBtn" onClick={this.deleteFile.bind(this)}>&times;</i>
        </td>
        <td className="contentLength">{contentLength}</td>
        <td>{generated}</td>
      </tr>
    );
  }
}

class FileList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      files: []
    };
  }
  reload() {
    let handler = data => this.setState({files: data});
    $.ajax('files/', {
      type: 'get',
      cache: false,
    }).done(handler);
  }
  componentDidMount() {
    this.reload();
    setInterval(() => this.reload(), this.props.pollInterval);
  }
  render() {
    if (this.state.files.length) {
      let fileNodes = this.state.files.map(function(file) {
        return (
          <File
              key={file.id}
              id={file.id}
              name={file.name}
              content_length={file.content_length}
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
}

module.exports = FileList;
