var authController = angular.module('authController', []);

authController.controller('LoginCtrl', [ 
        '$scope',
        '$rootScope',
        'vertxEventBusService',
		function($scope, $rootScope, vertxEventBusService) {

			$scope.$on("session-id-assigned", function(event) {
				console.log("sessionIDassigned");
				vertxEventBusService.on("esentri.login.reply:" + $rootScope.sessionId, function(message) {
					console.log("Received login reply. Payload is: " + message);
					var msg = angular.fromJson(message);
					$rootScope.loggedIn = msg.loggedIn;
					if(msg.loggedIn) {
						console.log("Login was successful. User logged in.");
						$rootScope.notification = "Hi, " + msg.user.name + " (ID: " + msg.user.id + ")";
						$('#login').css("display", "none");
						$('#logout').css("display", "block");
					} else {
						console.log("Login failed.");
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
					console.log("Sending login request with payload: " + cred);
					vertxEventBusService.send("esentri.login.request", angular.toJson(cred));
				}
			};
			
		}
]);

authController.controller('LogoutCtrl', [ '$scope', '$rootScope', 'vertxEventBusService', '$location',
		function($scope, $rootScope, vertxEventBusService, $location) {
			$scope.logout = function() {
				vertxEventBusService.send("esentri.logout", "logmeout").then(function(message) {
					console.log("Logout request sent, and answer received.");
					console.log("Payload for sending was irrelevant.");
					console.log("Answer is: " + message.body);
					msg = angular.fromJson(message.body);
					if(msg.logout) {
						$rootScope.loggedIn = false;
						$('#logout').css("display", "none");
						$('#login').css("display", "block");
						$location.path('phonebook');
					}
				});
			};
		} 
]);