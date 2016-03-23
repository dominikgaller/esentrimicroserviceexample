var phonebookController = angular.module('phonebookController', []);

phonebookController.controller('PhonebookCtrl', [
	'$scope',
	'$rootScope',
	'vertxEventBusService',
	function($scope, $rootScope, vertxEventBusService) {
		$scope.entries = $rootScope.entries;
		$scope.request = false;

		$scope.$on('vertx-eventbus.system.connected', function (event) {
			console.log("Eventbus connected.");
			if($rootScope.sessionId == 0) {
				console.log("Sending intial request for session id.");
				vertxEventBusService.send("esentri.session.request", "requesting session").then(function(message){
					//Body is needed here because msg is sync (format changes here)
					console.log("session id received. With payload: " + message.body);
					var json = angular.fromJson(message.body);
					$rootScope.sessionId = json.sessionId;
					var sObj = {
							sessionId: $rootScope.sessionId
					};
					$rootScope.sessionObj = angular.toJson(sObj);
					$rootScope.$broadcast("session-id-assigned");
					vertxEventBusService.addListener("esentri.entries.display:" + $rootScope.sessionId, function(message) {
						console.log("Received answer for entries.");
						console.log("Payload is: " + message);
						$scope.entries = JSON.parse(message);
						$rootScope.entries = $scope.entries;
					});
					console.log("Sending intial request for entries.");
					vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
					$scope.request = true;
				});
			} else {
				console.log("Sending request for entries.");
				vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
			    $scope.request = true;
			}
		});
		if(!$scope.request && $rootScope.sessionId != 0) {
			console.log("Sending request for entries.");
			vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
		}
		
	}
]);