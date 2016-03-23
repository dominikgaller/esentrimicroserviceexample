var editController = angular.module('editController', []);

editController.controller('EditCtrl', [
	'$scope',
	'$rootScope',
	'vertxEventBusService',
	function($scope, $rootScope, vertxEventBusService) {
		$scope.editrequest = false;
		$scope.entries = $rootScope.entries;
		$scope.$on('vertx-eventbus.system.connected', function (event) {
			console.log("Eventbus connected.");
			console.log("Sending initial request for entries. Payload is: " + $rootScope.sessionObj);
	        vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
	        $scope.editrequest = true;
		});
		
		if(!$scope.editrequest) {
			console.log("Sending request for entries. Payload is: " + $rootScope.sessionObj);
			vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
		}
		
		vertxEventBusService.addListener("esentri.entries.display:" + $rootScope.sessionId, function(message) {
			console.log("Received answer for entries.");
			console.log("Payload is: " + message);
			$scope.entries = angular.fromJson(message);
			$rootScope.entries = $scope.entries;
		});
		
		$scope.deleteEntry = function(entry) {
			var request = {
					entry: entry,
					sessionId: $rootScope.sessionId
			};
			console.log("Sending request for deleting an entry. Payload is: " + request);
			vertxEventBusService.send("esentri.entries.delete", angular.toJson(request));
		};
		
		$scope.addEntry = function(toBeAdded) {
			var request = {
					entry: toBeAdded,
					sessionId: $rootScope.sessionId
			};
			console.log("Sending request for adding an entry. Payload is: " + request);
			vertxEventBusService.send("esentri.entries.add", angular.toJson(request));
		}; 
	}
]);