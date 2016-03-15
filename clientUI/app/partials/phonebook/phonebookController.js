var phonebookController = angular.module('phonebookController', []);

phonebookController.controller('PhonebookCtrl', [
	'$scope',
	'$rootScope',
	'vertxEventBusService',
	function($scope, $rootScope, vertxEventBusService) {
		$scope.entries = $rootScope.entries;
		$scope.request = false;

		$scope.$on('vertx-eventbus.system.connected', function (event) {
			if($rootScope.sessionId == 0) {
				vertxEventBusService.send("esentri.session.request", "requesting session").then(function(message){
					var json = angular.fromJson(message);
					$rootScope.sessionId = json.sessionId;
					var sObj = {
							sessionId: $rootScope.sessionId
					};
					$rootScope.sessionObj = angular.toJson(sObj);
					$rootScope.$broadcast("sessionIDassigned");
					vertxEventBusService.addListener("esentri.entries.display:" + $rootScope.sessionId, function(message) {
						$scope.entries = JSON.parse(message);
						$rootScope.entries = $scope.entries;
					});
					vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
					$scope.request = true;
				});
			} else {
				vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
			    $scope.request = true;
			}
		});
		if(!$scope.request && $rootScope.sessionId != 0) {
			vertxEventBusService.send("esentri.entries.request", $rootScope.sessionObj);
		}
		
	}
]);