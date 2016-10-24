var React = require('react');
var ReactDOM = require('react-dom');
var $ = require('./jquery-2.1.4.min');

'use strict';

class Task extends React.Component {
  render() {
    var registered = new Date(this.props.registered) + '';
    return (
      <tr>
        <td>{this.props.fileName}</td>
        <td>{registered}</td>
      </tr>
    );
  }
}
class TaskList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      tasks: []
    };
  }
  reload() {
    let handler = data => this.setState({tasks: data});
    $.ajax('tasks/', {
      type: 'get',
      cache: false,
    }).done(handler);
  }
  componentDidMount() {
    this.reload();
    setInterval(() => this.reload(), this.props.pollInterval);
  }
  render() {
    if (this.state.tasks.length) {
      var taskNodes = this.state.tasks.map(function(task) {
        return (
          <Task
              key={task.id}
              fileName={task.fileName}
              registered={task.registered}>
          </Task>
        );
      });
      return (
        <div>
          <h2>Tasks</h2>
          <table className="table table-striped table-bordered">
            <thead>
              <tr>
                <th>file name</th>
                <th>registered time</th>
              </tr>
            </thead>
            <tbody>
              {taskNodes}
            </tbody>
          </table>
        </div>
      );
    } else {
      return (
        <div>
          <h2>Tasks</h2>
          <p className="text-info">
            No registered task
          </p>
        </div>
      );
    }
  }
}

module.exports = TaskList;
