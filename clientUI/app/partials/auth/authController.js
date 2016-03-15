var authController = angular.module('authController', []);

authController.controller('LoginCtrl', [ 
        '$scope',
        '$rootScope',
        'vertxEventBusService',
		function($scope, $rootScope, vertxEventBusService) {

			$scope.$on("sessionIDassigned", function(event) {
				vertxEventBusService.on("esentri.login.reply:" + $rootScope.sessionId, function(message) {
					console.log(message);
					var msg = angular.fromJson(message);
					$rootScope.loggedIn = msg.loggedIn;
					if(msg.loggedIn) {
						$rootScope.notification = "Hi, " + msg.user.name + " (ID: " + msg.user.id + ")";
						$('#login').css("display", "none");
						$('#logout').css("display", "block");
					} else {
						console.log("Else case");
						$rootScope.loginerr = "credentials wrong. try again.";
					}
				});
			});
			
			$scope.login = function(cred) {
				console.log(cred);
				if(cred === 'undefined') {
					$rootScope.loginerr = "please enter credentials.";
				} else {
					cred.sessionId = $rootScope.sessionId;
					console.log(cred);
					vertxEventBusService.send("esentri.login.request", angular.toJson(cred));
				}
			};
			
		}
]);

authController.controller('LogoutCtrl', [ '$scope', '$rootScope', 'vertxEventBusService',
		function($scope, $rootScope, vertxEventBusService) {
			$scope.logout = function() {
				vertxEventBusService.send("esentri.logout", "logmeout").then(function(message) {
					msg = angular.fromJson(message);
					if(msg.logout) {
						$rootScope.loggedIn = false;
						$('#logout').css("display", "none");
						$('#login').css("display", "block");
					}
				});
			};
		} 
]);