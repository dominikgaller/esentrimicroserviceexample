'use strict';
var app = angular.module('myApp', [
  'ngRoute',
  
  /* External Stuff */
  'knalli.angular-vertxbus',
  'ui.bootstrap',

  /* Controller */
  'phonebookController',
  'authController',
  'editController'
]);

app.config([
            '$routeProvider',
            'vertxEventBusProvider',
            function($routeProvider, vertxEventBusProvider) {
            	$routeProvider.when('/phonebook', {
            		templateUrl: 'partials/phonebook/phonebook.html',
            		controller: 'PhonebookCtrl'
            	})
            	.when('/edit', {
                    templateUrl: 'partials/edit/edit.html',
                    controller: 'EditCtrl',
                    resolve: {
                        factory: function ($rootScope, $location) {
                            if (!$rootScope.loggedIn) {
                            	$rootScope.loginerr = "login wrong!";
                                $location.path('/phonebook');
                            }
                            return $rootScope.loggedIn;
                        }
                    }
            	})
            	.otherwise({
            		redirectTo: '/phonebook'
            	});
            	
            	vertxEventBusProvider
            	.enable()
            	.useReconnect()
            	.useUrlServer('http://localhost:8080')
            	.useUrlPath('/eventbus')
            	.useDebug(true);
            }]);

app.run(['$rootScope', function($rootScope) {
	$rootScope.sessionId = 0;
}]);

