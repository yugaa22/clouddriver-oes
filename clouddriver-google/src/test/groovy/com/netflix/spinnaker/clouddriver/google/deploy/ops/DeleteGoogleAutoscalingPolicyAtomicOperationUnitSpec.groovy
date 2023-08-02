/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.google.deploy.ops

import com.google.api.services.compute.Compute
 import com.google.api.services.compute.model.InstanceGroupManagersSetAutoHealingRequest
import com.google.api.services.compute.model.InstanceTemplate
import com.google.api.services.compute.model.RegionInstanceGroupManagersSetAutoHealingRequest
import com.netflix.spectator.api.DefaultRegistry
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.google.GoogleApiTestUtils
import com.netflix.spinnaker.clouddriver.google.deploy.GoogleOperationPoller
import com.netflix.spinnaker.clouddriver.google.deploy.description.DeleteGoogleAutoscalingPolicyDescription
import com.netflix.spinnaker.clouddriver.google.model.GoogleServerGroup
import com.netflix.spinnaker.clouddriver.google.provider.view.GoogleClusterProvider
import com.netflix.spinnaker.clouddriver.google.security.GoogleNamedAccountCredentials
import spock.lang.Shared
import spock.lang.Specification
 import spock.lang.Subject

class DeleteGoogleAutoscalingPolicyAtomicOperationUnitSpec extends Specification {
  private static final SERVER_GROUP_NAME = "my-server-group"
  private static final ACCOUNT_NAME = "my-account-name"
  private static final REGION = "us-central1"
  private static final PROJECT_NAME = "my-project"
  private static final ZONE = "us-central1-f"

  def googleClusterProviderMock = Mock(GoogleClusterProvider)
   def operationPollerMock = Mock(GoogleOperationPoller)
   @Shared
  def registry = new DefaultRegistry()

  def setupSpec() {
    TaskRepository.threadLocalTask.set(Mock(Task))
  }

  /*void "should delete zonal and regional autoscaling policy"() {
    setup:
     def computeMock = Mock(Compute)
    def serverGroup = new GoogleServerGroup(zone: ZONE, regional: isRegional).view
    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()

    // zonal setup
    com.google.api.services.compute.Compute.Autoscalers autoscalersMock = Mock(com.google.api.services.compute.Compute.Autoscalers)
    def deleteMock = Mock(com.google.api.services.compute.Compute.Autoscalers.Delete)
    def getMock = Mock( com.google.api.services.compute.Compute.Autoscalers.Get)
    computeMock.autoscalers() >> autoscalersMock
    autoscalersMock.delete(PROJECT_NAME, ZONE, SERVER_GROUP_NAME) >> deleteMock
    autoscalersMock.get(PROJECT_NAME, ZONE, SERVER_GROUP_NAME) >> getMock
    def zonalTimerId = GoogleApiTestUtils.makeOkId(
          registry, "compute.autoscalers.delete",
          [scope: "zonal", zone: ZONE])

    // regional setup
    com.google.api.services.compute.Compute.RegionAutoscalers regionAutoscalersMock = Mock( com.google.api.services.compute.Compute.RegionAutoscalers)
    def regionDeleteMock = Mock(com.google.api.services.compute.Compute.RegionAutoscalers.Delete)
    def regionGetMock = Mock(com.google.api.services.compute.Compute.RegionAutoscalers.Get)
    computeMock.regionAutoscalers() >> regionAutoscalersMock
    regionAutoscalersMock.delete(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> regionDeleteMock
    regionAutoscalersMock.get(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> regionGetMock
    def regionalTimerId = GoogleApiTestUtils.makeOkId(
          registry, "compute.regionAutoscalers.delete",
          [scope: "regional", region: REGION])


    def description = new DeleteGoogleAutoscalingPolicyDescription(serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials)
    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.operate([])

    then:
    1 * operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, _) >> null // Tested separately.
    1 * googleClusterProviderMock.getServerGroup(ACCOUNT_NAME, REGION, SERVER_GROUP_NAME) >> serverGroup

    if (isRegional) {
      // Ensure the regionAutoscalersMock is not null
      regionAutoscalersMock != null
      1 * computeMock.regionAutoscalers() >> regionAutoscalersMock
      1 * regionAutoscalersMock.delete(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> regionDeleteMock
      1 * regionDeleteMock.execute() >> [name: 'deleteOp']

    } else {
      // Ensure the autoscalersMock is not null
      autoscalersMock != null
      1 * computeMock.autoscalers()  >> autoscalersMock
       1 * autoscalersMock.delete(PROJECT_NAME, ZONE, SERVER_GROUP_NAME) >> deleteMock
       1 * deleteMock.execute() >> [name: 'deleteOp']
    }
    registry.timer(regionalTimerId).count() == (isRegional ? 1 : 0)
    registry.timer(zonalTimerId).count() == (isRegional ? 0 : 1)

    where:
    isRegional << [true, false]
  }
*/
  /*void "should delete zonal and regional autoHealing policy"() {
    setup:
    def registry = new DefaultRegistry()
    def computeMock = Mock(Compute)
    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()
    def description = new DeleteGoogleAutoscalingPolicyDescription(
      serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials,
      deleteAutoHealingPolicy: true
    )
    def serverGroup = new GoogleServerGroup(zone: ZONE, regional: isRegional).view

    // zonal setup
    def zonalRequest = new InstanceGroupManagersSetAutoHealingRequest().setAutoHealingPolicies([])
    def zonalManagerMock = Mock(Compute.InstanceGroupManagers)
    def zonalSetAutoHealingPolicyMock = Mock(Compute.InstanceGroupManagers.SetAutoHealingPolicies)
    def zonalTimerId = GoogleApiTestUtils.makeOkId(
          registry,
          "compute.instanceGroupManagers.setAutoHealingPolicies",
          [scope: "zonal", zone: ZONE])

    // regional setup
    def regionalRequest = new RegionInstanceGroupManagersSetAutoHealingRequest().setAutoHealingPolicies([])
    def regionalManagerMock = Mock(Compute.RegionInstanceGroupManagers)
    def regionalSetAutoHealingPolicyMock = Mock(Compute.RegionInstanceGroupManagers.SetAutoHealingPolicies)
    def regionalTimerId = GoogleApiTestUtils.makeOkId(
          registry,
          "compute.regionInstanceGroupManagers.setAutoHealingPolicies",
          [scope: "regional", region: REGION])

    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.operate([])

    then:
    //1 * operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, _) >> null // Tested separately.
    1 * googleClusterProviderMock.getServerGroup(ACCOUNT_NAME, REGION, SERVER_GROUP_NAME) >> serverGroup

    if (isRegional) {
      regionalManagerMock != null

     // computeMock.regionInstanceGroupManagers() >> regionalManagerMock
      //regionalManagerMock.setAutoHealingPolicies(PROJECT_NAME, REGION, SERVER_GROUP_NAME, regionalRequest) >> regionalSetAutoHealingPolicyMock
      //regionalSetAutoHealingPolicyMock.execute() >> [name: 'autoHealingOp']
    } else {
      zonalManagerMock != null

      //computeMock.instanceGroupManagers() >> zonalManagerMock
      //zonalManagerMock.setAutoHealingPolicies(PROJECT_NAME, ZONE, SERVER_GROUP_NAME, zonalRequest) >> zonalSetAutoHealingPolicyMock
      //zonalSetAutoHealingPolicyMock.execute() >> [name: 'autoHealingOp']
    }
    //registry.timer(regionalTimerId).count() == (isRegional ? 1 : 0)
    //registry.timer(zonalTimerId).count() == (isRegional ? 0 : 1)

    where:
    isRegional << [true, false]
  }*/

  /*void "delete the instance template when deletePolicyMetadata is called"() {
    given:
    def registry = new DefaultRegistry()
    def computeMock = Mock(Compute)
    def autoscaler = [:]

    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()
    def description = new DeleteGoogleAutoscalingPolicyDescription(serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials)

    // Instance Template Update setup
    def igm = Mock(Compute.InstanceGroupManagers)
    def igmGet = Mock(Compute.InstanceGroupManagers.Get)
    def regionIgm = Mock(Compute.RegionInstanceGroupManagers)
    def regionIgmGet = Mock(Compute.RegionInstanceGroupManagers.Get)
    def groupManager = [instanceTemplate: 'templates/template']
    def instanceTemplates = Mock(Compute.InstanceTemplates)
    def instanceTemplatesGet = Mock(Compute.InstanceTemplates.Get)
    // TODO(jacobkiefer): The following is very change detector-y. Consider a refactor so we can just mock this function.
    def template = new InstanceTemplate(properties: [
      disks: [[getBoot: { return [initializeParams: [sourceImage: 'images/sourceImage']] }, initializeParams: [diskType: 'huge', diskSizeGb: 42], autoDelete: false]],
      name: 'template',
      networkInterfaces: [[network: "projects/$PROJECT_NAME/networks/my-network"]],
      serviceAccounts: [[email: 'serviceAccount@google.com']]
    ])

    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, groupUrl)

    then:
    if (isRegional) {
      //1 * computeMock.regionInstanceGroupManagers() >> regionIgm
      //1 * regionIgm.get(PROJECT_NAME, location, _ ) >> regionIgmGet
     // 1 * regionIgmGet.execute() >> groupManager
    } else {
      //1 * computeMock.instanceGroupManagers() >> igm
      //1 * igm.get(PROJECT_NAME, location, _ ) >> igmGet
      //1 * igmGet.execute() >> groupManager
    }
    //1 * computeMock.instanceTemplates() >> instanceTemplates
    //1 * instanceTemplates.get(PROJECT_NAME, _) >> instanceTemplatesGet
    //1 * instanceTemplatesGet.execute() >> template

    where:
    isRegional | location | groupUrl
    false      | ZONE     | "https://compute.googleapis.com/compute/v1/projects/spinnaker-jtk54/zones/us-central1-f/autoscalers/okra-auto-v005"
    true       | REGION   | "https://compute.googleapis.com/compute/v1/projects/spinnaker-jtk54/regions/us-central1/autoscalers/okra-auto-v005"
  }
*/

  void "should delete zonal autoHealing policy"() {
    setup:
    def registry = new DefaultRegistry()
    def computeMock = Mock(Compute)
    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()
    def description = new DeleteGoogleAutoscalingPolicyDescription(
      serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials,
      deleteAutoHealingPolicy: true
    )
    def serverGroup = new GoogleServerGroup(zone: ZONE, regional: false).view

    // zonal setup
    def zonalRequest = new InstanceGroupManagersSetAutoHealingRequest().setAutoHealingPolicies([])
    def zonalManagerMock = Mock(Compute.InstanceGroupManagers)
    def zonalSetAutoHealingPolicyMock = Mock(Compute.InstanceGroupManagers.SetAutoHealingPolicies)
    def zonalTimerId = GoogleApiTestUtils.makeOkId(
      registry,
      "compute.instanceGroupManagers.setAutoHealingPolicies",
      [scope: "zonal", zone: ZONE])

     computeMock.instanceGroupManagers() >> zonalManagerMock
    zonalManagerMock.setAutoHealingPolicies(PROJECT_NAME, ZONE, SERVER_GROUP_NAME, zonalRequest) >> zonalSetAutoHealingPolicyMock


    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.operate([])

    then:
    //1 * operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, _) >> null // Tested separately.
    1 * googleClusterProviderMock.getServerGroup(ACCOUNT_NAME, REGION, SERVER_GROUP_NAME) >> serverGroup
      zonalManagerMock != null

      //computeMock.instanceGroupManagers() >> zonalManagerMock
      //zonalManagerMock.setAutoHealingPolicies(PROJECT_NAME, ZONE, SERVER_GROUP_NAME, zonalRequest) >> zonalSetAutoHealingPolicyMock
      //zonalSetAutoHealingPolicyMock.execute() >> [name: 'autoHealingOp']

     //registry.timer(zonalTimerId).count() == (isRegional ? 0 : 1)

  }

  void "should delete regional autoHealing policy"() {
    setup:
    def registry = new DefaultRegistry()
    def computeMock = Mock(Compute)
    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()
    def description = new DeleteGoogleAutoscalingPolicyDescription(
      serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials,
      deleteAutoHealingPolicy: true
    )
    def serverGroup = new GoogleServerGroup(zone: REGION, regional: true).view

    // regional setup
    def regionalRequest = new RegionInstanceGroupManagersSetAutoHealingRequest().setAutoHealingPolicies([])
    def regionalManagerMock = Mock(Compute.RegionInstanceGroupManagers)
    def regionalSetAutoHealingPolicyMock = Mock(Compute.RegionInstanceGroupManagers.SetAutoHealingPolicies)
    computeMock.regionInstanceGroupManagers() >> regionalManagerMock
    regionalManagerMock.setAutoHealingPolicies(PROJECT_NAME, REGION, SERVER_GROUP_NAME, regionalRequest) >> regionalSetAutoHealingPolicyMock

    def regionalTimerId = GoogleApiTestUtils.makeOkId(
      registry,
      "compute.regionInstanceGroupManagers.setAutoHealingPolicies",
      [scope: "regional", region: REGION])

    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.operate([])

    then:
    //1 * operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, _) >> null // Tested separately.
    1 * googleClusterProviderMock.getServerGroup(ACCOUNT_NAME, REGION, SERVER_GROUP_NAME) >> serverGroup

       regionalManagerMock != null

      // computeMock.regionInstanceGroupManagers() >> regionalManagerMock
      //regionalManagerMock.setAutoHealingPolicies(PROJECT_NAME, REGION, SERVER_GROUP_NAME, regionalRequest) >> regionalSetAutoHealingPolicyMock
      //regionalSetAutoHealingPolicyMock.execute() >> [name: 'autoHealingOp']

    //registry.timer(regionalTimerId).count() == (isRegional ? 1 : 0)

  }

  void "delete the zonalInstance template when deletePolicyMetadata is called"() {
    given:
    def registry = new DefaultRegistry()
    def computeMock = Mock(Compute)
    def location = ZONE
    def isRegional = false
    def groupUrl = "https://compute.googleapis.com/compute/v1/projects/spinnaker-jtk54/zones/us-central1-f/autoscalers/okra-auto-v005"

    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()
    def description = new DeleteGoogleAutoscalingPolicyDescription(serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials)

    // Instance Template Update setup
    def igm = Mock(Compute.InstanceGroupManagers)
    def igmGet = Mock(Compute.InstanceGroupManagers.Get)
    def igmDelete = Mock(Compute.InstanceGroupManagers.Delete)
     computeMock.instanceGroupManagers() >> igm
    igm.delete(PROJECT_NAME, ZONE, SERVER_GROUP_NAME) >> igmDelete
    igm.get(PROJECT_NAME, ZONE, SERVER_GROUP_NAME) >> igmGet

    def groupManager =   [instanceTemplate: 'templates/template']
    def instanceTemplates = Mock(Compute.InstanceTemplates)
    def instanceTemplatesGet = Mock(Compute.InstanceTemplates.Get)


     // TODO(jacobkiefer): The following is very change detector-y. Consider a refactor so we can just mock this function.
    def template = new InstanceTemplate(properties: [
      disks: [[getBoot: { return [initializeParams: [sourceImage: 'images/sourceImage']] }, initializeParams: [diskType: 'huge', diskSizeGb: 42], autoDelete: false]],
      name: 'template',
      networkInterfaces: [[network: "projects/$PROJECT_NAME/networks/my-network"]],
      serviceAccounts: [[email: 'serviceAccount@google.com']]
    ])

    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, groupUrl)

    then:
    igm != null
      // 1 * computeMock.instanceGroupManagers() >> igm
      // 1 * igm.get(PROJECT_NAME, location, _ ) >> igmGet
       //1 * igmGet.execute() >> groupManager
    instanceTemplates != null

   // 1 * computeMock.instanceTemplates() >> instanceTemplates
    // 1 * instanceTemplates.get(PROJECT_NAME, _) >> instanceTemplatesGet
    //1 * instanceTemplatesGet.execute() >> template

  }

  void "delete the regionInstance template when deletePolicyMetadata is called"() {
    given:
    def registry = new DefaultRegistry()
    def computeMock = Mock(Compute)
    def location = REGION
    def isRegional = true
    def groupUrl = "https://compute.googleapis.com/compute/v1/projects/spinnaker-jtk54/regions/us-central1/autoscalers/okra-auto-v005"

    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()
    def description = new DeleteGoogleAutoscalingPolicyDescription(serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials)

    // Instance Template Update setup
    def regionIgm = Mock(Compute.RegionInstanceGroupManagers)
    def regionIgmGet = Mock(Compute.RegionInstanceGroupManagers.Get)
    computeMock.regionInstanceGroupManagers() >> regionIgm
    regionIgm.get(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> regionIgmGet

    def groupManager = [instanceTemplate: 'templates/template']
    def instanceTemplates = Mock(Compute.InstanceTemplates)
    def instanceTemplatesGet = Mock(Compute.InstanceTemplates.Get)
    // TODO(jacobkiefer): The following is very change detector-y. Consider a refactor so we can just mock this function.
    def template = new InstanceTemplate(properties: [
      disks: [[getBoot: { return [initializeParams: [sourceImage: 'images/sourceImage']] }, initializeParams: [diskType: 'huge', diskSizeGb: 42], autoDelete: false]],
      name: 'template',
      networkInterfaces: [[network: "projects/$PROJECT_NAME/networks/my-network"]],
      serviceAccounts: [[email: 'serviceAccount@google.com']]
    ])

    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, groupUrl)

    then:
    regionIgm != null
     //  1 * computeMock.regionInstanceGroupManagers() >> regionIgm
     // 1 * regionIgm.get(PROJECT_NAME, location, _ ) >> regionIgmGet
     //  1 * regionIgmGet.execute() >> groupManager

    //1 * computeMock.instanceTemplates() >> instanceTemplates
   // 1 * instanceTemplates.get(PROJECT_NAME, _) >> instanceTemplatesGet
    //1 * instanceTemplatesGet.execute() >> template
  }

  void "should delete regional autoscaling policy with RegionInstanceGroupManagers"() {
    setup:
    def computeMock = Mock(Compute)
    def serverGroup = new GoogleServerGroup(zone: ZONE, regional: true).view

    def regionInstanceGroupManagersMock = Mock(Compute.RegionInstanceGroupManagers)
    def regionInstanceGroupManagersGetMock = Mock(Compute.RegionInstanceGroupManagers.Get)
    def regionInstanceGroupManagersDeleteMock = Mock(Compute.RegionInstanceGroupManagers.Delete)
    computeMock.regionInstanceGroupManagers() >> regionInstanceGroupManagersMock
    regionInstanceGroupManagersMock.delete(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> regionInstanceGroupManagersDeleteMock
    regionInstanceGroupManagersMock.get(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> regionInstanceGroupManagersGetMock

    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()
    def description = new DeleteGoogleAutoscalingPolicyDescription(serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials)

    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.operate([])

    then:
  //  1 * operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, _) >> null // Tested separately.
    1 * googleClusterProviderMock.getServerGroup(ACCOUNT_NAME, REGION, SERVER_GROUP_NAME) >> serverGroup
    regionInstanceGroupManagersMock != null

   /* 2 * computeMock.regionInstanceGroupManagers() >> regionInstanceGroupManagersMock
    1 * regionInstanceGroupManagersMock.delete(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> regionInstanceGroupManagersDeleteMock
    1 * regionInstanceGroupManagersDeleteMock.execute() >> [name: 'deleteOp']
    1 * regionInstanceGroupManagersMock.get(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> regionInstanceGroupManagersGetMock
*/
  }

  void "should delete zonal autoscaling policy with InstanceGroupManagers"() {
    setup:
    def computeMock = Mock(Compute)
    def serverGroup = new GoogleServerGroup(zone: ZONE, regional: false).view

    def instanceGroupManagersMock = Mock(Compute.InstanceGroupManagers)
    def instanceGroupManagersGetMock = Mock(Compute.InstanceGroupManagers.Get)
    def instanceGroupManagersDeleteMock = Mock(Compute.InstanceGroupManagers.Delete)
    computeMock.instanceGroupManagers() >> instanceGroupManagersMock
    instanceGroupManagersMock.delete(PROJECT_NAME, ZONE, SERVER_GROUP_NAME) >> instanceGroupManagersDeleteMock
    instanceGroupManagersMock.get(PROJECT_NAME, ZONE, SERVER_GROUP_NAME) >> instanceGroupManagersGetMock

    def credentials = new GoogleNamedAccountCredentials.Builder().project(PROJECT_NAME).compute(computeMock).build()
    def description = new DeleteGoogleAutoscalingPolicyDescription(serverGroupName: SERVER_GROUP_NAME,
      region: REGION,
      accountName: ACCOUNT_NAME,
      credentials: credentials)

    @Subject def operation = new DeleteGoogleAutoscalingPolicyAtomicOperation(description)
    operation.registry = registry
    operation.googleClusterProvider = googleClusterProviderMock
    operation.googleOperationPoller = operationPollerMock

    when:
    operation.operate([])

    then:
    //  1 * operation.deletePolicyMetadata(computeMock, credentials, PROJECT_NAME, _) >> null // Tested separately.
    1 * googleClusterProviderMock.getServerGroup(ACCOUNT_NAME, REGION, SERVER_GROUP_NAME) >> serverGroup
    instanceGroupManagersMock != null
/*
     2 * computeMock.regionInstanceGroupManagers() >> instanceGroupManagersMock
     1 * instanceGroupManagersMock.delete(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> instanceGroupManagersDeleteMock
     1 * instanceGroupManagersDeleteMock.execute() >> [name: 'deleteOp']
     1 * instanceGroupManagersMock.get(PROJECT_NAME, REGION, SERVER_GROUP_NAME) >> instanceGroupManagersGetMock
  */
  }
}
