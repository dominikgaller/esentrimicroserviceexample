var editController = angular.module('editController', []);

editController.controller('EditCtrl', [
	'$scope',
	'$rootScope',
	'vertxEventBusService',
	function($scope, $rootScope, vertxEventBusService) {
		$scope.editrequest = false;
		$scope.entries = $rootScope.entries;
		$scope.$on('vertx-eventbus.system.connected', function (event) {
	        vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
	        $scope.editrequest = true;
		});
		
		if(!$scope.editrequest) {
			vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
		}
		
		vertxEventBusService.addListener("esentri.entries.display:" + $rootScope.sessionId, function(message) {
			$scope.entries = angular.fromJson(message);
			$rootScope.entries = $scope.entries;
		});
		
		$scope.deleteEntry = function(entry) {
			var request = {
					entry: entry,
					sessionId: $rootScope.sessionId
			};
			vertxEventBusService.send("esentri.entries.delete", angular.toJson(request));
		};
		
		$scope.addEntry = function(toBeAdded) {
			var request = {
					entry: toBeAdded,
					sessionId: $rootScope.sessionId
			};
			vertxEventBusService.send("esentri.entries.add", angular.toJson(request));
		}; 
	}
]);