var dashboardControllers = angular.module('dashboardControllers', []);


dashboardControllers.controller('MonitorController', function($scope, $log, $filter, $timeout, SebalImagesResource, TaskResource, appConfig) {
	
  $scope.isStarted = false;
  $scope.tasksStates = [];// {state: value.state, tasks: [{taskId : value.taskId , resourceId: value.resourceId , taskImages: []}]}
  $scope.sebalImages = [];
  $scope.tasks = [];
  $scope.imagesForTask = []; // {"taskId" : "taskId", "varImages": ["ndvi","ect"..."g"]}
  $scope.globalMsg = {msg:'',level:''};
  $scope.actualImage = '';
  $scope.warningTasks = false;
  $scope.showTasksModal = false;
  $scope.loading = false;
  $scope.renderImg = false;
  $scope.totalRunning = 0;
  $scope.totalReady = 0;
  $scope.totalCompleted = 0;
  $scope.totalFail = 0;
  
  function _arrayBufferToBase64( buffer ) {
	    $log.debug("Iniciando conversao");
	    var binary = '';
	    var bytes = new Uint8Array( buffer );
	    var len = bytes.byteLength;
	    $log.debug("Bytes length :"+bytes.byteLength);
	    for (var i = 0; i < len; i++) {
	        binary += String.fromCharCode( bytes[ i ] );
	    }
	    $log.debug("Fim conversao");
	    return window.btoa( binary );
  }

  $scope.getSebalImages = function() {
      $log.debug("Iniciando getImages");
      NProgress.start();
      //$timeout(function() {
      $scope.isStarted = true;
      $scope.globalMsg = {msg:'',level:''};
      SebalImagesResource.query(

          function(data) {
              $log.debug('Images: '+JSON.stringify(data));
              if(data.length < 1){
                  $scope.globalMsg={msg:'No found Sebal Images. Try again later.', level:'alert alert-warning'};
                  $scope.isStarted = false;
                  NProgress.done(false);
              }
              else{
                  $scope.warningImages=false; 
              }
              $scope.sebalImages = $filter('orderBy')(data, ['priority', 'name']);
              $scope.paginationItems = $scope.sebalImages;
              NProgress.done(true);
          },
          function(error){
              $scope.globalMsg={msg:'Error while trying to load Sebal Imagens from server'/*+JSON.stringify(error.config.url)*/, level:'alert alert-danger'};
              $log.error($scope.globalMsg.msg);
              $scope.isStarted = false;
              NProgress.done(false);
          }
      ); 

    //},2000);
      
      //Call with promisse exemple  
      //var imagesReturned = SebalImagesResource.query();
      //
      //imagesReturned.$promisse.then(function(result) {
      // 
      //    $log.debug('Images: '+JSON.stringify(data));
      //    $scope.sebalImages = data;
      //    $scope.paginationItems = $scope.sebalImages;
      //}); 

  };
  $scope.getSebalImages();

  $scope.getTaskForImage = function(imageName) {
      $log.info("Getting tasks for :"+imageName);
      $scope.loading = true;
      $scope.showTasksModal = true;

      $scope.actualImage = imageName;
      $scope.tasksStates = []; //Restart

      SebalImagesResource.get({ imgName: imageName }, function(data) {
          
          $scope.tasks = data;
          if($scope.tasks.length > 0){
            $scope.warningTasks = false;
            angular.forEach($scope.tasks, function(value, key) {
            	//new task
                var temp_task = {taskId : value.taskId , resourceId: value.resourceId , taskImages: []};

                var imageTaskState = $filter('filter')($scope.tasksStates, function (d) {return d.state === value.state;})[0];

                if(!angular.isDefined(imageTaskState)){
                  $log.debug('Add New Task Type: '+value.state);
                  var temp_tasksByState = [];
                  imageTaskState = {state: value.state, tasks: temp_tasksByState}
                  $scope.tasksStates.push(imageTaskState);
                  $log.debug('Add new Task State:'+JSON.stringify(imageTaskState));
                }

                $log.debug('Add New Task: '+JSON.stringify(temp_task));
                imageTaskState.tasks.push(temp_task);
            });
          }else{
            $scope.warningTasks = true;
          }

          $scope.loading = false;

      });
  };

  $scope.renderImagesForTask = function(tId) {
      $log.debug("Rendering image for task :"+tId);
      $scope.renderImg = true;
      $scope.actualTask = tId;
      TaskResource.get({ taskId: tId }, function(data) {
          if(data.variables.length > 0){

            var tempTaskImgs;

            angular.forEach($scope.tasksStates, function(value, key) {

                if(!angular.isDefined(tempTaskImgs)){
                  tempTaskImgs = $filter('filter')(value.tasks, function (d) {return d.taskId === tId;})[0];
                }

            });
             
            if(angular.isDefined(tempTaskImgs)){
              $log.debug("Getting images for task :"+tempTaskImgs.taskId);
              tempTaskImgs.taskImages = data.variables;
              $log.debug("Imgs Var :"+JSON.stringify(data.variables));
            }else{
              $log.erro("Task "+tId+" not founded");
            }
           
          }
          $scope.renderImg = false;
      });
  };

  $scope.getRenderedImageForTask = function(taskId, varType) {
      
      $scope.actualTask = taskId;

      var url = appConfig.urlSebalSchedulerService+"task/"+taskId+"/"+varType;
      window.open(url);
      // $log.debug("Get Rendered image "+varType+" for task :"+taskId+" from "+url);
     
      // $http.get(url, {responseType: "arraybuffer"}).
      //     success(function(data) {
      //     $log.debug("Getting image");
      //     $scope.imageFromServer = 'data:image/bmp;base64,' + _arrayBufferToBase64(data);
      // });

      // $scope.loading = false;

  };
  
  $scope.setRunningCount = function(value){
    $scope.totalRunning += value;
  }

  $scope.setReadyCount = function(value){
    $scope.totalReady += value;
  }

  $scope.setCompletedCount = function(value){
    $scope.totalCompleted += value;
  }

  $scope.setFailCount = function(value){
    $scope.totalFail += value;
  }

  $scope.closeWarningTasks = function() {
      $scope.warningTasks = false;
  }  

  $scope.refreshTasks = function() {
      if(angular.isDefined($scope.actualImage)){
        $scope.getTaskForImage($scope.actualImage);
      }
  }

  $scope.getStyleFromState = function(n) {

      // $log.debug("Getting style to "+n);
      
      var styleDefault = 'muted dark';
      var styleReady = 'warning';
      var styleRunning = 'muted dark';
      var styleSuccess = 'system';
      var styleFailed = 'danger';

      var stateReady = 'READY';
      var stateRunning = 'RUNNING';
      var stateSuccess = 'COMPLETED';
      var stateFailed = 'FAILED';

      var style = (n === stateReady   ? styleReady   : (
             n === stateRunning ? styleRunning : (
             n === stateSuccess ? styleSuccess : (
             n === stateFailed  ? styleFailed  : styleDefault))));


      //$log.debug("Style "+style);
      return style;
  }

  $scope.setLoading = function(state){
    $scope.loading = state;
  }
});

dashboardControllers.controller('dbImagesController', function($scope, $log, $filter, $timeout, $http, DbImagesResource, FilterResource, appConfig, uibDateParser) {
  
  $scope.isStarted = false;
  $scope.dbImages = [];
  $scope.globalMsg = {msg:'',level:''};
  $scope.loadingImg = false;
  $scope.imageName;
  $scope.imageDownloaded;
  $scope.imageBuff;

  $scope.filterStateValues = [];
  $scope.filterState = "";
  $scope.filterName = '';
  $scope.filterPeriodInit;
  $scope.filterPeriodEnd;
  
  function _arrayBufferToBase64( buffer ) {
      $log.debug("Iniciando conversao");
      var binary = '';
      var bytes = new Uint8Array( buffer );
      var len = bytes.byteLength;
      $log.debug("Bytes length :"+bytes.byteLength);
      for (var i = 0; i < len; i++) {
          binary += String.fromCharCode( bytes[ i ] );
      }
      $log.debug("Fim conversao");
      return window.btoa( binary );
  }

  $scope.getStateFilterValues = function() {
      $log.debug("Getting filter state values");
      NProgress.start();
      //$timeout(function() {
      $scope.isStarted = true;
      $scope.globalMsg = {msg:'',level:''};
      $scope.loadingImg = true;

      FilterResource.query(
          {filter:"image_state"},
          function(data) {
              $log.debug('States: '+JSON.stringify(data));
              if(data.length < 1){
                  $scope.globalMsg={msg:'Error while trying to get Image State values.'};
                  $scope.isStarted = false;
                  NProgress.done(false);
              }
              else{
                  $scope.warningImages=false; 
              }
              $scope.filterStateValues = $filter('orderBy')(data);
              NProgress.done(true);
              $scope.loadingImg = false;
          },
          function(error){
              $scope.globalMsg={msg:'Error while trying to get Image State values.'};
              $log.error($scope.globalMsg.msg);
              $scope.isStarted = false;
              NProgress.done(false);
              $scope.loadingImg = false;
          }
      );

  };
  $scope.getStateFilterValues();

  $scope.selectStateValue = function(value) {
    $log.debug('State selected: '+value);
    $('#state-filter-dropdown').removeClass("open");
    $scope.filterState = value;
    $('#state-filter-dropdown').attr('aria-expanded', false);

  };  

  $scope.getDbImages = function() {
      $log.debug("Iniciando getImages");
      NProgress.start();
      //$timeout(function() {
      $scope.isStarted = true;
      $scope.globalMsg = {msg:'',level:''};
      $scope.loadingImg = true;

      var dateInit = null;
      var dateEnd = null;

      if($scope.filterPeriodInit != null && (typeof $scope.filterPeriodInit) != "undefined"){
        dateInit = $scope.filterPeriodInit.getTime();
      }

      if($scope.filterPeriodEnd != null && (typeof  $scope.filterPeriodEnd) != "undefined"){
        dateEnd = $scope.filterPeriodEnd.getTime();
      }

       $log.debug("state:"+$scope.filterState+" - name:"+$scope.filterName+" - periodInit: "
        +dateInit+" - periodEnd: "+dateEnd);

      //Se tem datas.
      if(dateInit != null && dateEnd != null){
        if(dateInit >= dateEnd){
          $log.debug("Invalid dates");
          dateInit=null;
          dateEnd=null;
          $scope.globalMsg={msg:'Period initial date must be lesser than end date.', level:'alert alert-danger'};
          $scope.isStarted = false;
          NProgress.done(false);
          return;
        } 
      }

      if(dateInit == null && dateEnd != null){
        
        dateInit=null;
        dateEnd=null;
        $scope.globalMsg={msg:'Period initial date must be informed.', level:'alert alert-danger'};
        $scope.isStarted = false;
        NProgress.done(false);
        return;
       
      }

     

      DbImagesResource.query(
          {state:$scope.filterState, name:$scope.filterName, periodInit: dateInit, periodEnd: dateEnd},
          function(data) {
              $log.debug('Images: '+JSON.stringify(data));
              if(data.length < 1){
                  $scope.globalMsg={msg:'No found Sebal Images. Try again later.', level:'alert alert-warning'};
                  $scope.isStarted = false;
                  NProgress.done(false);
              }
              else{
                  $scope.warningImages=false; 
              }
              $scope.dbImages = $filter('orderBy')(data, ['priority', 'name']);
              $scope.paginationItems = $scope.dbImages;
              NProgress.done(true);
              $scope.loadingImg = false;
          },
          function(error){
              $scope.globalMsg={msg:'Error while trying to load Sebal Imagens from server'/*+JSON.stringify(error.config.url)*/, level:'alert alert-danger'};
              $log.error($scope.globalMsg.msg);
              $scope.isStarted = false;
              NProgress.done(false);
              $scope.loadingImg = false;
          }
      );

  };
  $scope.getDbImages();

  $scope.clearFilters = function() {
      $scope.filterState = "";
      $scope.filterName = '';
      $scope.filterPeriodInit = null;
      $scope.filterPeriodEnd = null;
  };  

  $scope.getImageFromRepository = function(imgName, varType) {
      $scope.loadingImg = true;
      $scope.errorMsg = {msg:'',level:''};
      $log.debug("Getting image "+imgName+varType);
      $scope.imageName=imgName+varType;
      var url = appConfig.urlSebalSchedulerService+appConfig.dbImageResourcePath+imgName+"/"+varType;
      //window.open(url);

      //DbImagesResource.query({ imgName: imageName },

      // $log.debug("Get Rendered image "+varType+" for task :"+taskId+" from "+url);
     
       $http.get(url, {responseType: "arraybuffer"}).
           success(function(data) {
               $log.debug("Getting image");
               $scope.imageBuff = data;
               $scope.imageDownloaded = 'data:image/bmp;base64,' + _arrayBufferToBase64(data);
               $scope.loadingImg = false;
           }).
           error(function(error){
              $log.error("Error on downlaod image");
              $scope.errorMsg={msg:'Error while trying to load Sebal Imagens from server'/*+JSON.stringify(error.config.url)*/, level:'alert alert-danger'};
              $scope.isStarted = false;
              NProgress.done(false);
              $scope.loadingImg = false;
          });
       

  };
 
  $scope.getStyleFromState = function(n) {

      // $log.debug("Getting style to "+n);
      
      var styleDefault = 'muted dark';
      var styleReady = 'warning';
      var styleRunning = 'muted dark';
      var styleSuccess = 'system';
      var styleFailed = 'danger';

      var stateReady = 'READY';
      var stateRunning = 'RUNNING';
      var stateSuccess = 'COMPLETED';
      var stateFailed = 'FAILED';

      var style = (n === stateReady   ? styleReady   : (
             n === stateRunning ? styleRunning : (
             n === stateSuccess ? styleSuccess : (
             n === stateFailed  ? styleFailed  : styleDefault))));


      //$log.debug("Style "+style);
      return style;
  }

  $scope.setLoading = function(state){
    $scope.loading = state;
  }

  $scope.download=function()
  {
     
      var arrayBufferView = new Uint8Array( $scope.imageBuff );
      var blob = new Blob( [ arrayBufferView ], { type: "image/jpg" } );
      var urlCreator = window.URL || window.webkitURL;
      var imageUrl = urlCreator.createObjectURL( blob );
     
      SaveToDisk(imageUrl,$scope.imageName);
   
  }
  function SaveToDisk(fileURL, fileName) {
        // for non-IE
        if (!window.ActiveXObject) {
            var save = document.createElement('a');
            save.href = fileURL;
            save.target = '_blank';
            save.download = fileName || 'unknown';

            var event = document.createEvent('Event');
            event.initEvent('click', true, true);
            save.dispatchEvent(event);
            (window.URL || window.webkitURL).revokeObjectURL(save.href);
        }

        // for IE
        else if ( !! window.ActiveXObject && document.execCommand)     {
            var _window = window.open(fileURL, '_blank');
            _window.document.close();
            _window.document.execCommand('SaveAs', true, fileName || fileURL)
            _window.close();
        }
  }

  $scope.dateOptions = {
    dateDisabled: disabled,
    formatYear: 'yyyy',
    formatDay: 'dd',
    formatMonth: 'MM',
    maxDate: new Date(2020, 5, 22),
    minDate: new Date(1900,1,1),
    startingDay: 1
  };

  // Disable weekend selection
  function disabled(data) {
    var date = data.date,
      mode = data.mode;
    return mode === 'day' && (date.getDay() === 0 || date.getDay() === 6);
  }


  $scope.openInit = function() {
    $scope.popupInit.opened = true;
  };

  $scope.openEnd = function() {
    $scope.popupEnd.opened = true;
  };

  $scope.formats = ['dd-MM-yyyy'];
  $scope.format = $scope.formats[0];
  $scope.altInputFormats = ['M!/d!/yyyy'];

  $scope.popupInit = {
    opened: false
  };

  $scope.popupEnd = {
    opened: false
  };

 

});



dashboardControllers.directive('modal', function () {
    return {
      template: '<div class="modal fade">' + 
          '<div class="tasks-modal-dialog">' + 
            '<div class="modal-content">' + 
              '<div class="modal-header">' + 
                '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' + 
                '<h4 class="modal-title">{{ title }}:{{ image }}</h4>' + 
              '</div>' + 
              '<div class="modal-body" ng-transclude></div>' + 
            '</div>' + 
          '</div>' + 
        '</div>',
      restrict: 'E',
      transclude: true,
      replace:true,
      scope: {
            title: '@',
            tasks: '=',
            tasksStates: '=',
            image: '=',
            warningTasks: '=warningTasks',
            visible: '='
      },
      link: function postLink(scope, element, attrs) {

        scope.title = attrs.title;

        scope.$watch('visible', function(value){
         
          if(value == true)
            $('#myModal').modal('show');
          else
            $('#myModal').modal('hide');
        });

        $(element).on('shown.bs.modal', function(){
          scope.$apply(function(){
            scope.visible = true;
          });
        });

        $(element).on('hidden.bs.modal', function(){
          scope.$apply(function(){
            scope.visible = false;
          });
        });
      }
    };
});


app.filter('offset', function() {
  return function(input, start) {
    start = parseInt(start, 10);
    return input.slice(start);
  };
});

dashboardControllers.controller("PaginationController", function($scope, $log) {

  $scope.itemsPerPage = 5;
  $scope.itemsPerPageOptions = [10, 15, 20, 50];
  $scope.currentPage = 0;
  $scope.prevPageDisabled = true;
  $scope.nextPageDisabled = true;
  $scope.offsetFilterCorrector = 0;

  var pageCount;

  $scope.filterTable = function () {
      $log.debug("Filtering table");
      $scope.offsetFilterCorrector = 0;
      var rex = new RegExp($('#filter').val(), 'i');

      $('.searchable tr').hide();
      $('.searchable tr').filter(function () {
          var filterResult = rex.test($(this).text());
          $log.debug("filterResult: "+filterResult);
          if(!filterResult){
            $scope.offsetFilterCorrector++;
          }
          $log.debug("offsetFilterCorrector: "+$scope.offsetFilterCorrector);
          return filterResult;
      }).show();


  };

  $scope.prevPage = function() {
    $scope.offsetFilterCorrector = 0;
    if ($scope.currentPage > 0) {
      $scope.currentPage--;
    }
    prevPageCheck();
    $('#filter').val('Search in table...');
  };
 
  $scope.pageCount = function(arrayElements) {
    
    pageCount = Math.ceil(arrayElements.length/$scope.itemsPerPage)-1;
    prevPageCheck();
    nextPageCheck();
    return pageCount;
  };

  $scope.getPages = function(arrayElements) {
    var pages = [];
    var range = $scope.pageCount(arrayElements)+1;
    for (var i = 0; i < range; i++) {
      pages.push(i+1);
    };
    return pages;
  };

  $scope.setPage = function(n) {
    $scope.offsetFilterCorrector = 0;
    $scope.currentPage = n;
    $('#filter').val('Search in table...');
    prevPageCheck();
    nextPageCheck();
  };

  $scope.nextPage = function(arrayElements) {
    $scope.offsetFilterCorrector = 0;
    if ($scope.currentPage < $scope.pageCount(arrayElements)) {
      $scope.currentPage++;
    }
    nextPageCheck();
    $('#filter').val('Search in table...');
  };

  $scope.selectItensPerPage = function(n){
    $scope.itemsPerPage = n;
  };

  function prevPageCheck(){
    if($scope.currentPage === 0){
        $scope.prevPageDisabled = true;
    }else{
        $scope.prevPageDisabled = false;
    }
  }

  function nextPageCheck(){
    if($scope.currentPage === pageCount){
        $scope.nextPageDisabled = true;
    }else{
        $scope.nextPageDisabled = false;
    }
  }
  



});


