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
import com.google.api.services.compute.model.Autoscaler
import com.google.api.services.compute.model.InstanceGroupManager
import com.google.api.services.compute.model.InstanceGroupManagersSetAutoHealingRequest
import com.google.api.services.compute.model.InstanceTemplate
import com.google.api.services.compute.model.RegionInstanceGroupManagersSetAutoHealingRequest
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.google.compute.RegionAutoscalers
import com.netflix.spinnaker.clouddriver.google.deploy.GCEUtil
import com.netflix.spinnaker.clouddriver.google.deploy.GoogleOperationPoller
import com.netflix.spinnaker.clouddriver.google.deploy.description.DeleteGoogleAutoscalingPolicyDescription
import com.netflix.spinnaker.clouddriver.google.model.GoogleServerGroup
import com.netflix.spinnaker.clouddriver.google.model.callbacks.Utils
import com.netflix.spinnaker.clouddriver.google.provider.view.GoogleClusterProvider
import com.netflix.spinnaker.clouddriver.google.security.GoogleNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperationsRegistry
import com.netflix.spinnaker.clouddriver.orchestration.OrchestrationProcessor
import org.springframework.beans.factory.annotation.Autowired
import com.google.api.services.compute.Compute.RegionAutoscalers
import com.google.api.services.compute.Compute.Autoscalers

class DeleteGoogleAutoscalingPolicyAtomicOperation extends GoogleAtomicOperation<Void>{

  private static final String BASE_PHASE = "DELETE_SCALING_POLICY"
  private final DeleteGoogleAutoscalingPolicyDescription description

  @Autowired
  private GoogleClusterProvider googleClusterProvider

  @Autowired
  private GoogleOperationPoller googleOperationPoller

  @Autowired
  AtomicOperationsRegistry atomicOperationsRegistry

  @Autowired
  OrchestrationProcessor orchestrationProcessor

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

  DeleteGoogleAutoscalingPolicyAtomicOperation(DeleteGoogleAutoscalingPolicyDescription description) {
    this.description = description
  }

  /**
   * Autoscaling policy:
   * curl -X POST -H "Content-Type: application/json" -d '[ { "deleteScalingPolicy": { "serverGroupName": "autoscale-regional", "credentials": "my-google-account", "region": "us-central1" }} ]' localhost:7002/gce/ops
   * curl -X POST -H "Content-Type: application/json" -d '[ { "deleteScalingPolicy": { "serverGroupName": "autoscale-zonal", "credentials": "my-google-account", "region": "us-central1" }} ]' localhost:7002/gce/ops
   *
   * AutoHealing policy:
   * curl -X POST -H "Content-Type: application/json" -d '[ { "deleteScalingPolicy": { "serverGroupName": "autoscale-zonal", "credentials": "my-google-account", "region": "us-central1", "deleteAutoHealingPolicy": true }} ]' localhost:7002/gce/ops
   */
  @Override
  Void operate(List priorOutputs) {


    def credentials = description.credentials
    def serverGroupName = description.serverGroupName
    def project = credentials.project
    def compute = credentials.compute
    def accountName = description.accountName
    def region = description.region
    def serverGroup = GCEUtil.queryServerGroup(googleClusterProvider, accountName, region, serverGroupName)
    def isRegional = serverGroup.regional
    def zone = serverGroup.zone

    if (description.deleteAutoHealingPolicy) {
      task.updateStatus BASE_PHASE, "Initializing deletion of autoHealing policy for $description.serverGroupName..."
      if (isRegional) {
        /*def request = new RegionInstanceGroupManagersSetAutoHealingRequest().setAutoHealingPolicies([])
        def deleteOp = timeExecute(
          compute.regionInstanceGroupManagers().setAutoHealingPolicies(project, region, serverGroupName, request),
          "compute.regionInstanceGroupManagers.setAutoHealingPolicies",
          TAG_SCOPE, SCOPE_REGIONAL, TAG_REGION, region)
        googleOperationPoller.waitForRegionalOperation(compute, project, region,
          deleteOp.getName(), null, task, "autoHealing policy for $serverGroupName", BASE_PHASE)
        deletePolicyMetadata(compute, credentials, project, GCEUtil.buildRegionalServerGroupUrl(project, region, serverGroupName))*/

        def request = new RegionInstanceGroupManagersSetAutoHealingRequest().setAutoHealingPolicies([])
        println("DEBUG1-setAutoHealingPolicies : [REGIONAL] : [compute=${compute}] : [regionInstanceGroupManagers()=${compute.regionInstanceGroupManagers()}] : [request()=${request}]")
        println("DEBUG1-setAutoHealingPolicies : [REGIONAL] : [project=${project}] : [region=${region}] : [serverGroupName=${serverGroupName}]")
        println("DEBUG1-setAutoHealingPolicies : [REGIONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_REGIONAL=${SCOPE_REGIONAL}] : [TAG_REGION=${TAG_REGION}] : [region=${region}]")
        Compute.RegionInstanceGroupManagers regionInstanceGroupManagers = compute.regionInstanceGroupManagers()
        def deleteOp = timeExecute(
          regionInstanceGroupManagers.setAutoHealingPolicies(project, region, serverGroupName, request),
          "compute.regionInstanceGroupManagers.setAutoHealingPolicies",
          TAG_SCOPE, SCOPE_REGIONAL, TAG_REGION, region)
        googleOperationPoller.waitForRegionalOperation(compute, project, region,
          /*deleteOp.getName()*/"deleteOp", null, task, "autoHealing policy for $serverGroupName", BASE_PHASE)
        deletePolicyMetadata(compute, credentials, project, GCEUtil.buildRegionalServerGroupUrl(project, region, serverGroupName))

      } else {
       /* def request = new InstanceGroupManagersSetAutoHealingRequest().setAutoHealingPolicies([])
        def deleteOp = timeExecute(
          compute.instanceGroupManagers().setAutoHealingPolicies(project, zone, serverGroupName, request),
          "compute.instanceGroupManagers.setAutoHealingPolicies",
          TAG_SCOPE, SCOPE_ZONAL, TAG_ZONE, zone)
        googleOperationPoller.waitForZonalOperation(compute, project, zone,
          deleteOp.getName(), null, task, "autoHealing policy for $serverGroupName", BASE_PHASE)
        deletePolicyMetadata(compute, credentials, project, GCEUtil.buildZonalServerGroupUrl(project, zone, serverGroupName))*/

        def request = new InstanceGroupManagersSetAutoHealingRequest().setAutoHealingPolicies([])
        println("DEBUG2-setAutoHealingPolicies : [ZONAL] : [compute=${compute}] : [instanceGroupManagers()=${compute.instanceGroupManagers()}] : [request()=${request}]")
        println("DEBUG2-setAutoHealingPolicies : [ZONAL] : [project=${project}] : [zone=${zone}] : [serverGroupName=${serverGroupName}]")
        println("DEBUG2-setAutoHealingPolicies : [ZONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_ZONAL=${SCOPE_ZONAL}] : [TAG_ZONE=${TAG_ZONE}] : [zone=${zone}]")
        Compute.InstanceGroupManagers instanceGroupManagers = compute.instanceGroupManagers()
        def deleteOp = timeExecute(
          instanceGroupManagers.setAutoHealingPolicies(project, zone, serverGroupName, request),
          "compute.instanceGroupManagers.setAutoHealingPolicies",
          TAG_SCOPE, SCOPE_ZONAL, TAG_ZONE, zone)
        googleOperationPoller.waitForRegionalOperation(compute, project, zone,
          /*deleteOp.getName()*/"deleteOp", null, task, "autoScaling policy for $serverGroupName", BASE_PHASE)
        deletePolicyMetadata(compute, credentials, project, GCEUtil.buildZonalServerGroupUrl(project, zone, serverGroupName))
      }
      task.updateStatus BASE_PHASE, "Done deleting autoHealing policy for $serverGroupName."
    } else {
      task.updateStatus BASE_PHASE, "Initializing deletion of scaling policy for $description.serverGroupName..."
      if (isRegional) {

        /*
          println("DEBUG1-delete : [REGIONAL] : [compute=${compute}] : [regionAutoscalers()=${compute.regionAutoscalers()}]")
        println("DEBUG1-delete : [REGIONAL] : [project=${project}] : [region=${region}] : [serverGroupName=${serverGroupName}]")
        println("DEBUG1-delete : [REGIONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_REGIONAL=${SCOPE_REGIONAL}] : [TAG_REGION=${TAG_REGION}] : [region=${region}]")

        def deleteOp = timeExecute(
            compute.regionAutoscalers().delete(project, region, serverGroupName),
            "compute.regionAutoscalers.delete",
            TAG_SCOPE, SCOPE_REGIONAL, TAG_REGION, region)
        googleOperationPoller.waitForRegionalOperation(compute, project, region,
          deleteOp.getName(), null, task, "autoScaling policy for $serverGroupName", BASE_PHASE)*/


        /*
          println("DEBUG1-delete : [REGIONAL] : [compute=${compute}] : [regionAutoscalers()=${compute.regionAutoscalers()}]")
        println("DEBUG1-delete : [REGIONAL] : [project=${project}] : [region=${region}] : [serverGroupName=${serverGroupName}]")
        println("DEBUG1-delete : [REGIONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_REGIONAL=${SCOPE_REGIONAL}] : [TAG_REGION=${TAG_REGION}] : [region=${region}]")

        com.google.api.services.compute.Compute.RegionAutoscalers regionAutoscalers = compute.regionAutoscalers()
        regionAutoscalers.delete(project, region, serverGroupName).execute()
        deletePolicyMetadata(compute, credentials, project, GCEUtil.buildRegionalServerGroupUrl(project, region, serverGroupName))*/

        println("DEBUG1-delete : [REGIONAL] : [compute=${compute}] : [regionInstanceGroupManagers()=${compute.regionInstanceGroupManagers()}]")
        println("DEBUG1-delete : [REGIONAL] : [project=${project}] : [region=${region}] : [serverGroupName=${serverGroupName}]")
        println("DEBUG1-delete : [REGIONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_REGIONAL=${SCOPE_REGIONAL}] : [TAG_REGION=${TAG_REGION}] : [region=${region}]")
        Compute.RegionInstanceGroupManagers regionInstanceGroupManagers = compute.regionInstanceGroupManagers()
        def deleteOp = timeExecute(
          regionInstanceGroupManagers.delete(project, region, serverGroupName ),
          "compute.regionInstanceGroupManagers.delete",
          TAG_SCOPE, SCOPE_REGIONAL, TAG_REGION, region)
        googleOperationPoller.waitForRegionalOperation(compute, project, region,
          /*deleteOp.getName()*/"deleteOp", null, task, "autoScaling policy for $serverGroupName", BASE_PHASE)
        deletePolicyMetadata(compute, credentials, project, GCEUtil.buildRegionalServerGroupUrl(project, region, serverGroupName))

      } else {

       /*
        println("DEBUG2-delete : [ZONAL] : [compute=${compute}] : [autoscalers()=${compute.autoscalers()}]")
        println("DEBUG2-delete : [ZONAL] : [project=${project}] : [zone=${zone}] : [serverGroupName=${serverGroupName}]")
        println("DEBUG2-delete : [ZONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_ZONAL=${SCOPE_ZONAL}] : [TAG_ZONE=${TAG_ZONE}] : [zone=${zone}]")
       def deleteOp = timeExecute(
            compute.autoscalers().delete(project, zone, serverGroupName),
            "compute.autoscalers.delete",
            TAG_SCOPE, SCOPE_ZONAL, TAG_ZONE, zone)
        googleOperationPoller.waitForZonalOperation(compute, project, zone,
          deleteOp.getName(), null, task, "autoScaling policy for $serverGroupName", BASE_PHASE)*/

       /* println("DEBUG2-delete : [ZONAL] : [compute=${compute}] : [autoscalers()=${compute.autoscalers()}]")
        println("DEBUG2-delete : [ZONAL] : [project=${project}] : [zone=${zone}] : [serverGroupName=${serverGroupName}]")
        println("DEBUG2-delete : [ZONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_ZONAL=${SCOPE_ZONAL}] : [TAG_ZONE=${TAG_ZONE}] : [zone=${zone}]")
        com.google.api.services.compute.Compute.Autoscalers autoscalers = compute.autoscalers()
        autoscalers.delete(project, zone, serverGroupName).execute()
        deletePolicyMetadata(compute, credentials, project, GCEUtil.buildZonalServerGroupUrl(project, zone, serverGroupName))*/
        println("DEBUG2-delete : [ZONAL] : [compute=${compute}] : [instanceGroupManagers()=${compute.instanceGroupManagers()}]")
        println("DEBUG2-delete : [ZONAL] : [project=${project}] : [zone=${zone}] : [serverGroupName=${serverGroupName}]")
        println("DEBUG2-delete : [ZONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_ZONAL=${SCOPE_ZONAL}] : [TAG_ZONE=${TAG_ZONE}] : [zone=${zone}]")
        Compute.InstanceGroupManagers instanceGroupManagers = compute.instanceGroupManagers()
        def deleteOp = timeExecute(
          instanceGroupManagers.delete(project, zone, serverGroupName ),
          "compute.instanceGroupManagers.delete",
          TAG_SCOPE, SCOPE_REGIONAL, TAG_REGION, zone)
        googleOperationPoller.waitForRegionalOperation(compute, project, zone,
          /*deleteOp.getName()*/"deleteOp", null, task, "autoScaling policy for $serverGroupName", BASE_PHASE)
        deletePolicyMetadata(compute, credentials, project, GCEUtil.buildZonalServerGroupUrl(project, zone, serverGroupName))
      }
      task.updateStatus BASE_PHASE, "Done deleting scaling policy for $serverGroupName."
    }

    return null
  }

  void deletePolicyMetadata(Compute compute,
                            GoogleNamedAccountCredentials credentials,
                            String project,
                            String groupUrl) {
    def groupName = Utils.getLocalName(groupUrl)
    String templateUrl = null
    def groupRegion = Utils.getRegionFromGroupUrl(groupUrl)


      switch (Utils.determineServerGroupType(groupUrl)) {
        case GoogleServerGroup.ServerGroupType.REGIONAL:

          /*templateUrl = timeExecute(
            compute.regionAutoscalers().get(project, groupRegion, groupName),
            "compute.regionAutoscalers.get",
            TAG_SCOPE, SCOPE_REGIONAL, TAG_REGION, groupRegion)
            .getInstanceTemplate()*/

         /* println("DEBUG1-get : [REGIONAL] : [compute=${compute}] : [regionAutoscalers()=${compute.regionAutoscalers()}]")
          println("DEBUG1-get : [REGIONAL] : [project=${project}] : [groupRegion=${groupRegion}] : [groupName=${groupName}]")
          println("DEBUG1-get : [REGIONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_REGIONAL=${SCOPE_REGIONAL}] : [TAG_REGION=${TAG_REGION}] : [groupRegion=${groupRegion}]")

          com.google.api.services.compute.Compute.RegionAutoscalers regionAutoscalers = compute.regionAutoscalers()
          def regionAutoscalerList = regionAutoscalers.get(project, groupRegion, groupName).execute()

          if (regionAutoscalerList != null) {
            templateUrl = regionAutoscalerList.getInstanceTemplate()
            println "DEBUG1-get : [REGIONAL] : [regionAutoscalers Name: ${regionAutoscalerList.getName()}]"
            println "DEBUG1-get : [REGIONAL] : [regionAutoscalers Target: ${regionAutoscalerList.getTarget()}]"
            println "DEBUG1-get : [REGIONAL] : [Instance Template Link: ${templateUrl}]"
          } else {
            println "DEBUG1-get : [REGIONAL] : [regionAutoscalers ${regionAutoscalerList} not found in the specified region and project.]"
          }*/
          println("DEBUG1-get : [REGIONAL] : [compute=${compute}] : [regionInstanceGroupManagers()=${compute.regionInstanceGroupManagers()}]")
          println("DEBUG1-get : [REGIONAL] : [project=${project}] : [groupRegion=${groupRegion}] : [groupName=${groupName}]")
          println("DEBUG1-get : [REGIONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_REGIONAL=${SCOPE_REGIONAL}] : [TAG_REGION=${TAG_REGION}] : [groupRegion=${groupRegion}]")

         /* templateUrl = timeExecute(
            compute.regionInstanceGroupManagers().get(project, groupRegion, groupName),
            "compute.regionInstanceGroupManagers.get",
            TAG_SCOPE, SCOPE_REGIONAL, TAG_REGION, groupRegion)
            .getInstanceTemplate()*/
          com.google.api.services.compute.Compute.RegionInstanceGroupManagers regionInstanceGroupManagers = compute.regionInstanceGroupManagers()
          Compute.RegionInstanceGroupManagers.Get getOperation = regionInstanceGroupManagers.get(project, groupRegion, groupName)
          if(getOperation != null){
            def regionInstanceGroupManagerList = getOperation.execute()

            if (regionInstanceGroupManagerList != null) {
              templateUrl = regionInstanceGroupManagerList.getInstanceTemplate()
              println "DEBUG1-get : [REGIONAL] : [regionInstanceGroupManagerList Name: ${regionInstanceGroupManagerList.getName()}]"
              println "DEBUG1-get : [REGIONAL] : [regionInstanceGroupManagerList Target: ${regionInstanceGroupManagerList.getTarget()}]"
              println "DEBUG1-get : [REGIONAL] : [Instance Template Link: ${templateUrl}]"
            } else {
              println "DEBUG1-get : [REGIONAL] : [regionInstanceGroupManagerList ${regionInstanceGroupManagerList} not found in the specified region and project.]"
            }
          }else {
            println "DEBUG1-get : [REGIONAL] : [getOperation ${getOperation} not found in the specified region and project.]"
          }

          break
        case GoogleServerGroup.ServerGroupType.ZONAL:
          def groupZone = Utils.getZoneFromGroupUrl(groupUrl)
          /*def groupZone = Utils.getZoneFromGroupUrl(groupUrl)
          templateUrl = timeExecute(
            compute.instanceGroupManagers().get(project, groupZone, groupName),
            "compute.instanceGroupManagers.get",
            TAG_SCOPE, SCOPE_ZONAL, TAG_ZONE, groupZone)
            .getInstanceTemplate()*/
/*
          println("DEBUG2-get : [ZONAL] : [compute=${compute}] : [autoscalers()=${compute.autoscalers()}]")
          println("DEBUG2-get : [ZONAL] : [project=${project}] : [groupZone=${groupZone}] : [groupName=${groupName}]")
          println("DEBUG2-get : [ZONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_ZONAL=${SCOPE_ZONAL}] : [TAG_ZONE=${TAG_ZONE}] : [groupZone=${groupZone}]")


          com.google.api.services.compute.Compute.Autoscalers autoscalers = compute.autoscalers()
          def autoscalerList = autoscalers.get(project, groupZone, groupName).execute()

          if (autoscalerList != null) {
            println "DEBUG2-get : [ZONAL] : [Autoscaler Name: ${autoscalerList.getName()}]"
            println "DEBUG2-get : [ZONAL] : [Autoscaler Target: ${autoscalerList.getTarget()}]"
            templateUrl = autoscalerList.getInstanceTemplate()
            println "DEBUG2-get : [ZONAL] : [Instance Template Link: ${templateUrl}]"
          } else {
            println "DEBUG2-get : [ZONAL] : [Autoscalers ${autoscalerList} not found in the specified region and project.]"
          }*/

          println("DEBUG2-get : [ZONAL] : [compute=${compute}] : [instanceGroupManagers()=${compute.instanceGroupManagers()}]")
          println("DEBUG2-get : [ZONAL] : [project=${project}] : [groupZone=${groupZone}] : [groupName=${groupName}]")
          println("DEBUG2-get : [ZONAL] : [TAG_SCOPE=${TAG_SCOPE}] : [SCOPE_ZONAL=${SCOPE_ZONAL}] : [TAG_ZONE=${TAG_ZONE}] : [groupZone=${groupZone}]")

          /*  templateUrl = timeExecute(
            compute.instanceGroupManagers().get(project, groupZone, groupName),
            "compute.instanceGroupManagers.get",
            TAG_SCOPE, SCOPE_ZONAL, TAG_ZONE, groupZone)
            .getInstanceTemplate()*/
          com.google.api.services.compute.Compute.InstanceGroupManagers instanceGroupManagers = compute.instanceGroupManagers()
          Compute.InstanceGroupManagers.Get getOperation = instanceGroupManagers.get(project, groupZone, groupName)
          if(getOperation != null) {
            def instanceGroupManagerList = getOperation.execute()

            if (instanceGroupManagerList != null) {
              templateUrl = instanceGroupManagerList.getInstanceTemplate()
              println "DEBUG2-get : [ZONAL] : [instanceGroupManagerList Name: ${instanceGroupManagerList.getName()}]"
              println "DEBUG2-get : [ZONAL] : [instanceGroupManagerList Target: ${instanceGroupManagerList.getTarget()}]"
              println "DEBUG2-get : [ZONAL] : [Instance Template Link: ${templateUrl}]"
            } else {
              println "DEBUG2-get : [ZONAL] : [instanceGroupManagerList ${instanceGroupManagerList} not found in the specified region and project.]"
            }
          }else {
            println "DEBUG2-get : [ZONAL] : [getOperation ${getOperation} not found in the specified region and project.]"
          }
          break
        default:
          throw new IllegalStateException("Server group referenced by ${groupUrl} has illegal type.")
          break
      }

    if(templateUrl != null){
      def instanceTemplateName = "your_instance_template_name"

      // Call the get() method to retrieve the instance template
      InstanceTemplate template = compute.instanceTemplates().get(project, instanceTemplateName).execute()

      /*  InstanceTemplate template = timeExecute(
          compute.instanceTemplates().get(project, Utils.getLocalName(templateUrl)),
          "compute.instancesTemplates.get",
          TAG_SCOPE, SCOPE_GLOBAL)*/

      // Use the instance template details as needed
      if (template != null) {
        println "Instance Template Name: ${template.getName()}"
        println "Instance Template Self Link: ${template.getSelfLink()}"

        def instanceDescription = GCEUtil.buildInstanceDescriptionFromTemplate(project, template)

        def templateOpMap = [
          image              : instanceDescription.image,
          instanceType       : instanceDescription.instanceType,
          credentials        : credentials.getName(),
          disks              : instanceDescription.disks,
          instanceMetadata   : instanceDescription.instanceMetadata,
          tags               : instanceDescription.tags,
          network            : instanceDescription.network,
          subnet             : instanceDescription.subnet,
          serviceAccountEmail: instanceDescription.serviceAccountEmail,
          authScopes         : instanceDescription.authScopes,
          preemptible        : instanceDescription.preemptible,
          automaticRestart   : instanceDescription.automaticRestart,
          onHostMaintenance  : instanceDescription.onHostMaintenance,
          region             : groupRegion,
          serverGroupName    : groupName
        ]

        if (instanceDescription.minCpuPlatform) {
          templateOpMap.minCpuPlatform = instanceDescription.minCpuPlatform
        }

        if (templateOpMap?.instanceMetadata) {
          templateOpMap.instanceMetadata.remove(GCEUtil.AUTOSCALING_POLICY)
          def converter = atomicOperationsRegistry.getAtomicOperationConverter('modifyGoogleServerGroupInstanceTemplateDescription', 'gce')
          AtomicOperation templateOp = converter.convertOperation(templateOpMap)
          orchestrationProcessor.process('gce', [templateOp], UUID.randomUUID().toString())
        }
      } else {
        println "Instance Template not found in the specified project."
      }
    }else {
      println "Instance Template Link not found in the specified project."
    }

  }
}
