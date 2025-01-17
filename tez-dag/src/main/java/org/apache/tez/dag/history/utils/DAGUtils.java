/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tez.dag.history.utils;

import org.apache.tez.common.counters.CounterGroup;
import org.apache.tez.common.counters.TezCounter;
import org.apache.tez.common.counters.TezCounters;
import org.apache.tez.dag.api.records.DAGProtos;
import org.apache.tez.dag.api.records.DAGProtos.PlanGroupInputEdgeInfo;
import org.apache.tez.dag.app.dag.impl.VertexStats;
import org.apache.tez.dag.records.TezTaskID;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DAGUtils {

  public static JSONObject generateSimpleJSONPlan(DAGProtos.DAGPlan dagPlan) throws JSONException {

    final String DAG_NAME_KEY = "dagName";
    final String VERTICES_KEY = "vertices";
    final String EDGES_KEY = "edges";

    final String VERTEX_NAME_KEY = "vertexName";
    final String PROCESSOR_CLASS_KEY = "processorClass";
    final String IN_EDGE_IDS_KEY = "inEdgeIds";
    final String OUT_EDGE_IDS_KEY = "outEdgeIds";
    final String ADDITIONAL_INPUTS_KEY = "additionalInputs";
    final String ADDITIONAL_OUTPUTS_KEY = "additionalOutputs";
    final String VERTEX_MANAGER_PLUGIN_CLASS_KEY =
        "vertexManagerPluginClass";

    final String EDGE_ID_KEY = "edgeId";
    final String INPUT_VERTEX_NAME_KEY = "inputVertexName";
    final String OUTPUT_VERTEX_NAME_KEY = "outputVertexName";
    final String DATA_MOVEMENT_TYPE_KEY = "dataMovementType";
    final String DATA_SOURCE_TYPE_KEY = "dataSourceType";
    final String SCHEDULING_TYPE_KEY = "schedulingType";
    final String EDGE_SOURCE_CLASS_KEY = "edgeSourceClass";
    final String EDGE_DESTINATION_CLASS_KEY =
        "edgeDestinationClass";

    final String NAME_KEY = "name";
    final String CLASS_KEY = "class";
    final String INITIALIZER_KEY = "initializer";

    JSONObject dagJson = new JSONObject();
    dagJson.put(DAG_NAME_KEY, dagPlan.getName());
    for (DAGProtos.VertexPlan vertexPlan : dagPlan.getVertexList()) {
      JSONObject vertexJson = new JSONObject();
      vertexJson.put(VERTEX_NAME_KEY, vertexPlan.getName());

      if (vertexPlan.hasProcessorDescriptor()) {
        vertexJson.put(PROCESSOR_CLASS_KEY,
            vertexPlan.getProcessorDescriptor().getClassName());
      }

      for (String inEdgeId : vertexPlan.getInEdgeIdList()) {
        vertexJson.accumulate(IN_EDGE_IDS_KEY, inEdgeId);
      }
      for (String outEdgeId : vertexPlan.getOutEdgeIdList()) {
        vertexJson.accumulate(OUT_EDGE_IDS_KEY, outEdgeId);
      }

      for (DAGProtos.RootInputLeafOutputProto input :
          vertexPlan.getInputsList()) {
        JSONObject jsonInput = new JSONObject();
        jsonInput.put(NAME_KEY, input.getName());
        jsonInput.put(CLASS_KEY, input.getEntityDescriptor().getClassName());
        if (input.hasInitializerClassName()) {
          jsonInput.put(INITIALIZER_KEY, input.getInitializerClassName());
        }
        vertexJson.accumulate(ADDITIONAL_INPUTS_KEY, jsonInput);
      }

      for (DAGProtos.RootInputLeafOutputProto output :
          vertexPlan.getOutputsList()) {
        JSONObject jsonOutput = new JSONObject();
        jsonOutput.put(NAME_KEY, output.getName());
        jsonOutput.put(CLASS_KEY, output.getEntityDescriptor().getClassName());
        if (output.hasInitializerClassName()) {
          jsonOutput.put(INITIALIZER_KEY, output.getInitializerClassName());
        }
        vertexJson.accumulate(ADDITIONAL_OUTPUTS_KEY, jsonOutput);
      }

      if (vertexPlan.hasVertexManagerPlugin()) {
        vertexJson.put(VERTEX_MANAGER_PLUGIN_CLASS_KEY,
            vertexPlan.getVertexManagerPlugin().getClassName());
      }

      dagJson.accumulate(VERTICES_KEY, vertexJson);
    }

    for (DAGProtos.EdgePlan edgePlan : dagPlan.getEdgeList()) {
      JSONObject edgeJson = new JSONObject();
      edgeJson.put(EDGE_ID_KEY, edgePlan.getId());
      edgeJson.put(INPUT_VERTEX_NAME_KEY, edgePlan.getInputVertexName());
      edgeJson.put(OUTPUT_VERTEX_NAME_KEY, edgePlan.getOutputVertexName());
      edgeJson.put(DATA_MOVEMENT_TYPE_KEY,
          edgePlan.getDataMovementType().name());
      edgeJson.put(DATA_SOURCE_TYPE_KEY, edgePlan.getDataSourceType().name());
      edgeJson.put(SCHEDULING_TYPE_KEY, edgePlan.getSchedulingType().name());
      edgeJson.put(EDGE_SOURCE_CLASS_KEY,
          edgePlan.getEdgeSource().getClassName());
      edgeJson.put(EDGE_DESTINATION_CLASS_KEY,
          edgePlan.getEdgeDestination().getClassName());

      dagJson.accumulate(EDGES_KEY, edgeJson);
    }

    return dagJson;
  }

  public static JSONObject convertCountersToJSON(TezCounters counters)
      throws JSONException {
    JSONObject jsonObject = new JSONObject();
    if (counters == null) {
      return jsonObject;
    }

    for (CounterGroup group : counters) {
      JSONObject jsonCGrp = new JSONObject();
      jsonCGrp.put(ATSConstants.COUNTER_GROUP_NAME, group.getName());
      jsonCGrp.put(ATSConstants.COUNTER_GROUP_DISPLAY_NAME,
          group.getDisplayName());
      for (TezCounter counter : group) {
        JSONObject counterJson = new JSONObject();
        counterJson.put(ATSConstants.COUNTER_NAME, counter.getName());
        counterJson.put(ATSConstants.COUNTER_DISPLAY_NAME,
            counter.getDisplayName());
        counterJson.put(ATSConstants.COUNTER_VALUE, counter.getValue());
        jsonCGrp.accumulate(ATSConstants.COUNTERS, counterJson);
      }
      jsonObject.accumulate(ATSConstants.COUNTER_GROUPS, jsonCGrp);
    }
    return jsonObject;
  }

  public static Map<String,Object> convertCountersToATSMap(TezCounters counters) {
    Map<String,Object> object = new LinkedHashMap<String, Object>();
    if (counters == null) {
        return object;
      }
    ArrayList<Object> counterGroupsList = new ArrayList<Object>();
    for (CounterGroup group : counters) {
        Map<String,Object> counterGroupMap = new LinkedHashMap<String, Object>();
        counterGroupMap.put(ATSConstants.COUNTER_GROUP_NAME, group.getName());
        counterGroupMap.put(ATSConstants.COUNTER_GROUP_DISPLAY_NAME,
                group.getDisplayName());
        ArrayList<Object> counterList = new ArrayList<Object>();
        for (TezCounter counter : group) {
            Map<String,Object> counterMap = new LinkedHashMap<String, Object>();
            counterMap.put(ATSConstants.COUNTER_NAME, counter.getName());
            counterMap.put(ATSConstants.COUNTER_DISPLAY_NAME,
                    counter.getDisplayName());
            counterMap.put(ATSConstants.COUNTER_VALUE, counter.getValue());
            counterList.add(counterMap);
          }
        putInto(counterGroupMap, ATSConstants.COUNTERS, counterList);
        counterGroupsList.add(counterGroupMap);
      }
    putInto(object, ATSConstants.COUNTER_GROUPS, counterGroupsList);
    return object;
  }

  public static Map<String,Object> convertDAGPlanToATSMap(
      DAGProtos.DAGPlan dagPlan) {

    final String VERSION_KEY = "version";
    final int version = 1;
    final String DAG_NAME_KEY = "dagName";
    final String VERTICES_KEY = "vertices";
    final String EDGES_KEY = "edges";
    final String VERTEX_GROUPS_KEY = "vertexGroups";

    final String VERTEX_NAME_KEY = "vertexName";
    final String PROCESSOR_CLASS_KEY = "processorClass";
    final String IN_EDGE_IDS_KEY = "inEdgeIds";
    final String OUT_EDGE_IDS_KEY = "outEdgeIds";
    final String ADDITIONAL_INPUTS_KEY = "additionalInputs";
    final String ADDITIONAL_OUTPUTS_KEY = "additionalOutputs";
    final String VERTEX_MANAGER_PLUGIN_CLASS_KEY =
        "vertexManagerPluginClass";

    final String EDGE_ID_KEY = "edgeId";
    final String INPUT_VERTEX_NAME_KEY = "inputVertexName";
    final String OUTPUT_VERTEX_NAME_KEY = "outputVertexName";
    final String DATA_MOVEMENT_TYPE_KEY = "dataMovementType";
    final String DATA_SOURCE_TYPE_KEY = "dataSourceType";
    final String SCHEDULING_TYPE_KEY = "schedulingType";
    final String EDGE_SOURCE_CLASS_KEY = "edgeSourceClass";
    final String EDGE_DESTINATION_CLASS_KEY =
        "edgeDestinationClass";

    final String NAME_KEY = "name";
    final String CLASS_KEY = "class";
    final String INITIALIZER_KEY = "initializer";

    final String VERTEX_GROUP_NAME_KEY = "groupName";
    final String VERTEX_GROUP_MEMBERS_KEY = "groupMembers";
    final String VERTEX_GROUP_OUTPUTS_KEY = "outputs";
    final String VERTEX_GROUP_EDGE_MERGED_INPUTS_KEY = "edgeMergedInputs";
    final String VERTEX_GROUP_DESTINATION_VERTEX_NAME_KEY = "destinationVertexName";

    Map<String,Object> dagMap = new LinkedHashMap<String, Object>();
    dagMap.put(DAG_NAME_KEY, dagPlan.getName());
    dagMap.put(VERSION_KEY, version);
    ArrayList<Object> verticesList = new ArrayList<Object>();
    for (DAGProtos.VertexPlan vertexPlan : dagPlan.getVertexList()) {
      Map<String,Object> vertexMap = new LinkedHashMap<String, Object>();
      vertexMap.put(VERTEX_NAME_KEY, vertexPlan.getName());

      if (vertexPlan.hasProcessorDescriptor()) {
        vertexMap.put(PROCESSOR_CLASS_KEY,
            vertexPlan.getProcessorDescriptor().getClassName());
      }

      ArrayList<Object> inEdgeIdList = new ArrayList<Object>();
      inEdgeIdList.addAll(vertexPlan.getInEdgeIdList());
      putInto(vertexMap, IN_EDGE_IDS_KEY, inEdgeIdList);

      ArrayList<Object> outEdgeIdList = new ArrayList<Object>();
      outEdgeIdList.addAll(vertexPlan.getOutEdgeIdList());
      putInto(vertexMap, OUT_EDGE_IDS_KEY, outEdgeIdList);

      ArrayList<Object> inputsList = new ArrayList<Object>();
      for (DAGProtos.RootInputLeafOutputProto input :
          vertexPlan.getInputsList()) {
        Map<String,Object> inputMap = new LinkedHashMap<String, Object>();
        inputMap.put(NAME_KEY, input.getName());
        inputMap.put(CLASS_KEY, input.getEntityDescriptor().getClassName());
        if (input.hasInitializerClassName()) {
          inputMap.put(INITIALIZER_KEY, input.getInitializerClassName());
        }
        inputsList.add(inputMap);
      }
      putInto(vertexMap, ADDITIONAL_INPUTS_KEY, inputsList);

      ArrayList<Object> outputsList = new ArrayList<Object>();
      for (DAGProtos.RootInputLeafOutputProto output :
          vertexPlan.getOutputsList()) {
        Map<String,Object> outputMap = new LinkedHashMap<String, Object>();
        outputMap.put(NAME_KEY, output.getName());
        outputMap.put(CLASS_KEY, output.getEntityDescriptor().getClassName());
        if (output.hasInitializerClassName()) {
          outputMap.put(INITIALIZER_KEY, output.getInitializerClassName());
        }
        outputsList.add(outputMap);
      }
      putInto(vertexMap, ADDITIONAL_OUTPUTS_KEY, outputsList);

      if (vertexPlan.hasVertexManagerPlugin()) {
        vertexMap.put(VERTEX_MANAGER_PLUGIN_CLASS_KEY,
            vertexPlan.getVertexManagerPlugin().getClassName());
      }

      verticesList.add(vertexMap);
    }
    putInto(dagMap, VERTICES_KEY, verticesList);

    ArrayList<Object> edgesList = new ArrayList<Object>();
    for (DAGProtos.EdgePlan edgePlan : dagPlan.getEdgeList()) {
      Map<String,Object> edgeMap = new LinkedHashMap<String, Object>();
      edgeMap.put(EDGE_ID_KEY, edgePlan.getId());
      edgeMap.put(INPUT_VERTEX_NAME_KEY, edgePlan.getInputVertexName());
      edgeMap.put(OUTPUT_VERTEX_NAME_KEY, edgePlan.getOutputVertexName());
      edgeMap.put(DATA_MOVEMENT_TYPE_KEY,
          edgePlan.getDataMovementType().name());
      edgeMap.put(DATA_SOURCE_TYPE_KEY, edgePlan.getDataSourceType().name());
      edgeMap.put(SCHEDULING_TYPE_KEY, edgePlan.getSchedulingType().name());
      edgeMap.put(EDGE_SOURCE_CLASS_KEY,
          edgePlan.getEdgeSource().getClassName());
      edgeMap.put(EDGE_DESTINATION_CLASS_KEY,
          edgePlan.getEdgeDestination().getClassName());

      edgesList.add(edgeMap);
    }
    putInto(dagMap, EDGES_KEY, edgesList);

    ArrayList<Object> vertexGroupsList = new ArrayList<Object>();
    for (DAGProtos.PlanVertexGroupInfo vertexGroupInfo :
        dagPlan.getVertexGroupsList()) {
      Map<String,Object> groupMap = new LinkedHashMap<String, Object>();
      groupMap.put(VERTEX_GROUP_NAME_KEY, vertexGroupInfo.getGroupName());
      if (vertexGroupInfo.getGroupMembersCount() > 0 ) {
        groupMap.put(VERTEX_GROUP_MEMBERS_KEY, vertexGroupInfo.getGroupMembersList());
      }
      if (vertexGroupInfo.getOutputsCount() > 0) {
        groupMap.put(VERTEX_GROUP_OUTPUTS_KEY, vertexGroupInfo.getOutputsList());
      }

      if (vertexGroupInfo.getEdgeMergedInputsCount() > 0) {
        ArrayList<Object> edgeMergedInputs = new ArrayList<Object>();
        for (PlanGroupInputEdgeInfo edgeMergedInputInfo :
            vertexGroupInfo.getEdgeMergedInputsList()) {
          Map<String,Object> edgeMergedInput = new LinkedHashMap<String, Object>();
          edgeMergedInput.put(VERTEX_GROUP_DESTINATION_VERTEX_NAME_KEY,
              edgeMergedInputInfo.getDestVertexName());
          if (edgeMergedInputInfo.hasMergedInput()
            && edgeMergedInputInfo.getMergedInput().hasClassName()) {
            edgeMergedInput.put(PROCESSOR_CLASS_KEY,
                edgeMergedInputInfo.getMergedInput().getClassName());
          }
          edgeMergedInputs.add(edgeMergedInput);
        }
        groupMap.put(VERTEX_GROUP_EDGE_MERGED_INPUTS_KEY, edgeMergedInputs);
      }
      vertexGroupsList.add(groupMap);
    }
    putInto(dagMap, VERTEX_GROUPS_KEY, vertexGroupsList);

    return dagMap;
  }

  private static void putInto(Map<String, Object> map, String key,
      ArrayList<Object> list) {
    if (list.isEmpty()) {
      return;
    }
    map.put(key, list);
  }

  private static ArrayList<String> convertToStringArrayList(
      Collection<TezTaskID> collection) {
    ArrayList<String> list = new ArrayList<String>(collection.size());
    for (TezTaskID t : collection) {
      list.add(t.toString());
    }
    return list;
  }

  public static Map<String,Object> convertVertexStatsToATSMap(
      VertexStats vertexStats) {
    Map<String,Object> vertexStatsMap = new LinkedHashMap<String, Object>();
    if (vertexStats == null) {
      return vertexStatsMap;
    }

    final String FIRST_TASK_START_TIME_KEY = "firstTaskStartTime";
    final String FIRST_TASKS_TO_START_KEY = "firstTasksToStart";
    final String LAST_TASK_FINISH_TIME_KEY = "lastTaskFinishTime";
    final String LAST_TASKS_TO_FINISH_KEY = "lastTasksToFinish";

    final String MIN_TASK_DURATION = "minTaskDuration";
    final String MAX_TASK_DURATION = "maxTaskDuration";
    final String AVG_TASK_DURATION = "avgTaskDuration";

    final String SHORTEST_DURATION_TASKS = "shortestDurationTasks";
    final String LONGEST_DURATION_TASKS = "longestDurationTasks";

    vertexStatsMap.put(FIRST_TASK_START_TIME_KEY, vertexStats.getFirstTaskStartTime());
    if (vertexStats.getFirstTasksToStart() != null
        && !vertexStats.getFirstTasksToStart().isEmpty()) {
      vertexStatsMap.put(FIRST_TASKS_TO_START_KEY,
          convertToStringArrayList(vertexStats.getFirstTasksToStart()));
    }
    vertexStatsMap.put(LAST_TASK_FINISH_TIME_KEY, vertexStats.getLastTaskFinishTime());
    if (vertexStats.getLastTasksToFinish() != null
        && !vertexStats.getLastTasksToFinish().isEmpty()) {
      vertexStatsMap.put(LAST_TASKS_TO_FINISH_KEY,
          convertToStringArrayList(vertexStats.getLastTasksToFinish()));
    }

    vertexStatsMap.put(MIN_TASK_DURATION, vertexStats.getMinTaskDuration());
    vertexStatsMap.put(MAX_TASK_DURATION, vertexStats.getMaxTaskDuration());
    vertexStatsMap.put(AVG_TASK_DURATION, vertexStats.getAvgTaskDuration());

    if (vertexStats.getShortestDurationTasks() != null
        && !vertexStats.getShortestDurationTasks().isEmpty()) {
      vertexStatsMap.put(SHORTEST_DURATION_TASKS,
          convertToStringArrayList(vertexStats.getShortestDurationTasks()));
    }
    if (vertexStats.getLongestDurationTasks() != null
        && !vertexStats.getLongestDurationTasks().isEmpty()) {
      vertexStatsMap.put(LONGEST_DURATION_TASKS,
          convertToStringArrayList(vertexStats.getLongestDurationTasks()));
    }

    return vertexStatsMap;
  }

}
