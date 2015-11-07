'use strict';
function setupForm() {
  'use strict';
  var $form = $('#post-video').submit(function(e){
    'use strict';
    e.preventDefault();
    var formData = new FormData($form[0]);
    $.ajax(action, {
      processData: false,
      type: 'post',
      contentType: false,
      dataType: 'text',
      data: formData
    })
    .done(function(data) {
      'use strict';
      $result.addClass('info').removeClass('warning').text(data).show();
    })
    .fail(function() {
      'use strict';
      $result.addClass('warning').removeClass('info').text('failed to register').show();
    });
    return false;
  });
  var $result = $form.find('#result');
  var action = $form.attr('action');
}
function loadTasks() {
  'use strict';
  var $row = $('#tasks');
  var $tasks = $row.find('tbody');
  $.ajax('tasks/', {
    type: 'get'
  })
  .done(function(data) {
    'use strict';
    function displayTable() {
        var flagment = document.createDocumentFragment();
        $.each(data, function(i, task) {
          'use strict';
          var $tr = $('<tr>').data('key', task['key']).append(
            $('<td>').text(task['fileName'])
          ).append(
            $('<td>').text(new Date(task['registered']))
          );
          flagment.appendChild($tr[0]);
        });
        $tasks[0].appendChild(flagment);
        $tasks.closest('table').show();
    }
    if (data.length === 0) {
      $row.find('p').show();
    } else {
      displayTable();
    }
  });
}
setupForm();
loadTasks();
