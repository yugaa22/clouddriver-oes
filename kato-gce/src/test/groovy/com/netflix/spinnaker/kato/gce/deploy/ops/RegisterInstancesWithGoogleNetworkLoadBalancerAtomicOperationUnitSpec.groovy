/*
 * Copyright 2015 Google, Inc.
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

package com.netflix.spinnaker.kato.gce.deploy.ops

import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.ForwardingRule
import com.google.api.services.compute.model.ForwardingRuleList
import com.google.api.services.compute.model.Instance
import com.google.api.services.compute.model.InstanceAggregatedList
import com.google.api.services.compute.model.InstanceReference
import com.google.api.services.compute.model.InstancesScopedList
import com.google.api.services.compute.model.TargetPoolsAddInstanceRequest
import com.netflix.spinnaker.amos.gce.GoogleCredentials
import com.netflix.spinnaker.kato.gce.deploy.GCEResourceNotFoundException
import com.netflix.spinnaker.kato.data.task.Task
import com.netflix.spinnaker.kato.data.task.TaskRepository
import com.netflix.spinnaker.kato.gce.deploy.description.RegisterInstancesWithGoogleNetworkLoadBalancerDescription
import spock.lang.Specification
import spock.lang.Subject

class RegisterInstancesWithGoogleNetworkLoadBalancerAtomicOperationUnitSpec extends Specification {
  private static final ACCOUNT_NAME = "auto"
  private static final PROJECT_NAME = "my_project"
  private static final LOAD_BALANCER_NAME_1 = "spinnaker-test-lb1"
  private static final TARGET_POOL_NAME_1 = "spinnaker-target-pool1"
  private static final LOAD_BALANCER_NAME_2 = "spinnaker-test-lb2"
  private static final TARGET_POOL_NAME_2 = "spinnaker-target-pool2"
  private static final REGION = "us-central1"
  private static final ZONE = "$REGION-a"
  private static final INSTANCE_ID1 = "my-app7-dev-v000-instance1"
  private static final INSTANCE_ID2 = "my-app7-dev-v000-instance2"
  private static final INSTANCE_URL1 =
    "https://www.googleapis.com/compute/v1/projects/$PROJECT_NAME/zones/$ZONE/instances/$INSTANCE_ID1"
  private static final INSTANCE_URL2 =
    "https://www.googleapis.com/compute/v1/projects/$PROJECT_NAME/zones/$ZONE/instances/$INSTANCE_ID2"

  private static final INSTANCE_IDS = [INSTANCE_ID1, INSTANCE_ID2]
  private static final INSTANCE_URLS = [INSTANCE_URL1, INSTANCE_URL2]

  def setupSpec() {
    TaskRepository.threadLocalTask.set(Mock(Task))
  }

  void "should register instances"() {
    setup:
      def computeMock = Mock(Compute)
      def forwardingRulesMock = Mock(Compute.ForwardingRules)
      def listForwardingRulesMock = Mock(Compute.ForwardingRules.List)
      def forwardingRulesListReal = new ForwardingRuleList(items:[
        new ForwardingRule(
          name: LOAD_BALANCER_NAME_1,
          target: TARGET_POOL_NAME_1
        ),
        new ForwardingRule(
          name: LOAD_BALANCER_NAME_2,
          target: TARGET_POOL_NAME_2
        )
      ])
      def instancesMock = Mock(Compute.Instances)
      def instancesAggregatedListMock = Mock(Compute.Instances.AggregatedList)
      def instance1 = new Instance(name: INSTANCE_ID1, selfLink: INSTANCE_URL1)
      def instance2 = new Instance(name: INSTANCE_ID2, selfLink: INSTANCE_URL2)
      def zoneToInstanceMap = [
        "zones/$ZONE": new InstancesScopedList(instances: [instance1, instance2])
      ]
      def instanceAggregatedListReal = new InstanceAggregatedList(items: zoneToInstanceMap)
      def targetPoolsMock = Mock(Compute.TargetPools)
      def addInstanceMock = Mock(Compute.TargetPools.AddInstance)

      def credentials = new GoogleCredentials(PROJECT_NAME, computeMock)
      def description = new RegisterInstancesWithGoogleNetworkLoadBalancerDescription(
          networkLoadBalancerNames: [LOAD_BALANCER_NAME_1, LOAD_BALANCER_NAME_2],
          instanceIds: INSTANCE_IDS,
          region: REGION,
          accountName: ACCOUNT_NAME,
          credentials: credentials)
      @Subject def operation = new RegisterInstancesWithGoogleNetworkLoadBalancerAtomicOperation(description)

      def request = new TargetPoolsAddInstanceRequest()
      request.instances = INSTANCE_URLS.collect { url -> new InstanceReference(instance: url) }

    when:
      operation.operate([])

    then:
      1 * computeMock.forwardingRules() >> forwardingRulesMock
      1 * forwardingRulesMock.list(PROJECT_NAME, REGION) >> listForwardingRulesMock
      1 * listForwardingRulesMock.execute() >> forwardingRulesListReal
    then:
      1 * computeMock.instances() >> instancesMock
      1 * instancesMock.aggregatedList(PROJECT_NAME) >> instancesAggregatedListMock
      1 * instancesAggregatedListMock.execute() >> instanceAggregatedListReal
    then:
      1 * computeMock.targetPools() >> targetPoolsMock
      1 * targetPoolsMock.addInstance(PROJECT_NAME, REGION, TARGET_POOL_NAME_1, request) >> addInstanceMock
      1 * addInstanceMock.execute()
    then:
      1 * computeMock.targetPools() >> targetPoolsMock
      1 * targetPoolsMock.addInstance(PROJECT_NAME, REGION, TARGET_POOL_NAME_2, request) >> addInstanceMock
      1 * addInstanceMock.execute()
  }

  void "throws ResourceNotFound with unknown load balancer"() {
    setup:
      def computeMock = Mock(Compute)
      def forwardingRulesMock = Mock(Compute.ForwardingRules)
      def listForwardingRulesMock = Mock(Compute.ForwardingRules.List)
      def forwardingRulesListReal = new ForwardingRuleList(items:[
        new ForwardingRule(
          name: "AnotherLoadBalancerName",
          target: TARGET_POOL_NAME_1
        )
      ])

      def credentials = new GoogleCredentials(PROJECT_NAME, computeMock)
      def description = new RegisterInstancesWithGoogleNetworkLoadBalancerDescription(
          networkLoadBalancerNames: [LOAD_BALANCER_NAME_1],
          instanceIds: INSTANCE_IDS,
          region: REGION,
          accountName: ACCOUNT_NAME,
          credentials: credentials)
      @Subject def operation = new RegisterInstancesWithGoogleNetworkLoadBalancerAtomicOperation(description)

      def request = new TargetPoolsAddInstanceRequest()
      request.instances = INSTANCE_URLS.collect { url -> new InstanceReference(instance: url) }

    when:
      operation.operate([])

    then:
      1 * computeMock.forwardingRules() >> forwardingRulesMock
      1 * forwardingRulesMock.list(PROJECT_NAME, REGION) >> listForwardingRulesMock
      1 * listForwardingRulesMock.execute() >> forwardingRulesListReal
      thrown GCEResourceNotFoundException
  }
}
