var app = angular.module('schedulerDashboard', [
	'dashboardControllers',
	'dashboardServices',
	'ui.bootstrap'
]).config(function($logProvider){
  $logProvider.debugEnabled(true);
}).constant("appConfig", {
	"urlSebalSchedulerService":"http://localhost:9192/sebal-scheduler/",
	"imageResourcePath":"image/:imgName",
	"taskResourcePath":"task/:taskId/:varType",
	"dbImageResourcePath":"fetcher/image/",
	"filterResourcePath":"fetcher/filter/:filter"
}
);

//Template for Angular code organization
//var phonecatApp = angular.module('phonecatApp', [
//  'ngRoute',
//  'phonecatAnimations',
//
//  'phonecatControllers',
//  'phonecatFilters',
//  'phonecatServices'
//]);
//
//phonecatApp.config(['$routeProvider',
//  function($routeProvider) {
//    $routeProvider.
//      when('/phones', {
//        templateUrl: 'partials/phone-list.html',
//        controller: 'PhoneListCtrl'
//      }).
//      when('/phones/:phoneId', {
//        templateUrl: 'partials/phone-detail.html',
//        controller: 'PhoneDetailCtrl'
//      }).
//      otherwise({
//        redirectTo: '/phones'
//      });
//  }]);
