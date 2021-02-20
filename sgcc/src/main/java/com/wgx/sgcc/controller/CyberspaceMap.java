package com.gjdw.stserver.taskutils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gjdw.stserver.model.*;
import com.gjdw.stserver.model.vo.DicBusiCodeVO;
import com.gjdw.stserver.robustness.SendRequest;
import com.gjdw.stserver.service.DicBusiService;
import com.gjdw.stserver.service.serviceImpl.*;
import com.gjdw.stserver.taskutils.models.*;
import com.gjdw.stserver.util.DicBusiEnum;
import com.gjdw.stserver.util.IpUtils;
import com.gjdw.stserver.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.System;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 网络空间测绘
 * */
@Slf4j
@Service
public class CyberspaceMap implements TaskInterface {

    @Autowired
    private MappingTaskServiceImpl mappingTaskServiceImpl;
    @Autowired
    private MappingTaskRecordServiceImpl mappingTaskRecordServiceImpl;
    @Autowired
    private MappingTaskRecordIpServiceImpl mappingTaskRecordIpServiceImpl;
    @Autowired
    private MappingTaskRecordPortServiceImpl mappingTaskRecordPortServiceImpl;
    @Autowired
    private MappingTaskRecordPortRuleServiceImpl mappingTaskRecordPortRuleServiceImpl;
    @Autowired
    private IpInfoServiceImpl ipInfoServiceImpl;
    @Autowired
    private MappingObjInfoServiceImpl mappingObjInfoServiceImpl;
    @Autowired
    private PortInfoServiceImpl portInfoServiceImpl;
    @Autowired
    private PortRuleServiceImpl portRuleServiceImpl;
    @Autowired
    private PortGroupInfoServiceImpl portGroupInfoServiceImpl;
    @Autowired
    private TaskUtilsConfig taskUtilsConfig;
    @Autowired
    private PenetrationScan penetrationScan;
    @Autowired
    private SendRequest sendRequest;
    @Autowired
    private TempTaskServiceImpl tempTaskServiceImpl;
    @Autowired
    private DataPackageServiceImpl dataPackageServiceImpl;
    @Autowired
    private CompanyIpDetailsInfoServiceImpl companyIpDetailsInfoServiceImpl;
    @Autowired
    private ToolInfoServiceImpl toolInfoServiceImpl;
    /**
     * 字典业务 Service
     */
    @Autowired
    DicBusiService dicBusiService;

    // 任务下发锁
    private ReentrantLock lock = new ReentrantLock();
    // 任务接收锁
    private ReentrantLock receivelock = new ReentrantLock();

    private Logger logger = LoggerFactory.getLogger(CyberspaceMap.class);

    private KafkaConsumer kafkaConsumer;
    private SimpleDateFormat simpleDateFormat;

    public CyberspaceMap() {
        System.out.println("======================================= 空间测绘任务");
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 测绘任务下发
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String TaskPush(String taskId) {
        return TaskPush(taskId, "");
    }

    /**
     * 测绘任务下发
     */
    @Transactional(rollbackFor = Exception.class)
    public String TaskPush(String taskId,String userId){
        System.out.println("action：网络空间测绘任务下发；task ID：" + taskId);
        String message = "";
        try {
            lock.lock();
            /*String getUrl = taskUtilsConfig.getRequestUrl() +
                    "/api/penetration/heartBeatStatus?taskType=0" +
                    "&taskId=" + taskId;
            String result = HttpClientUtil.doGet(getUrl);
            String code = getTaskStatus(result);
            if(code.equals("")){
                ResponseModel responseModel = new ResponseModel();
                responseModel.error_code = -2;
                responseModel.error_message = "failed";
                message = JsonMapper.obj2String(responseModel);
                return message;
            }*/
            MappingTask mappingTaskInfo = getMappingTaskById(taskId);
            if(mappingTaskInfo == null){
                ResponseModel responseModel = new ResponseModel();
                responseModel.error_code = -1;
                responseModel.error_message = "任务不存在";
                message = JsonMapper.obj2String(responseModel);
                return message;
            }
            List<ToolInfo> toolInfoList = toolInfoServiceImpl.selectAll();
            if(toolInfoList.size() <= 0) {
                ResponseModel responseModel = new ResponseModel();
                responseModel.error_code = -4;
                responseModel.error_message = "当前没有可用的服务器";
                message = JsonMapper.obj2String(responseModel);
                return message;
            }
            // 判断任务状态，避免重复发送
            if(mappingTaskInfo.getTaskStatus() != null && mappingTaskInfo.getTaskStatus().equals("1")) {
                System.out.println("当前存在执行中的任务，请等待任务结束！");
                logger.error("当前存在执行中的任务，请等待任务结束！");
                return message;
            }
            List<String> ipStrList = new ArrayList<>();
            List<MappingObjInfo> mappingObjInfoList = mappingObjInfoServiceImpl.selectByTaskId(taskId);
            if (!mappingTaskInfo.getScanObjType().equals("0")) {
                // IP/IP段/指定资产
                for (MappingObjInfo mappingObjInfo : mappingObjInfoList) {
                    String val = mappingObjInfo.getObjValue();
                    if (mappingObjInfo.getObjType().equals("1")) {
                        // IP段
                        if (val.indexOf("-") > -1) {
                            // ip段 192.168.0.1 - 192.168.0.19
                            ipStrList.addAll(new ArrayList(Arrays.asList(IpUtils.IpSegParsing(val))));
                        } else if (val.indexOf("/") > -1) {
                            // cidr 192.168.0.1/18
                            ipStrList.addAll(Arrays.asList(IpUtils.CrdiParsing(val)));
                        }
                    } else if (mappingObjInfo.getObjType().equals("0")) {
                        // IP
                        if (!ipStrList.contains(val)) {
                            ipStrList.add(val);
                        }
                    } else if (mappingObjInfo.getObjType().equals("2")) {
                        // 指定资产
                        List<String> assetids = new ArrayList<>();
                        for (String assetid : val.split(",")) {
                            assetids.add(assetid);
                        }
                        List<IpInfo> ipInfoList = ipInfoServiceImpl.selectByAssetId(assetids);
                        for (IpInfo ipInfo : ipInfoList) {
                            ipStrList.add(ipInfo.getIp());
                        }
                    }
                }
            } else {
                // 全网段扫描
                List<CompanyIpDetailsInfo> companyIpDetailsInfoList =
                        companyIpDetailsInfoServiceImpl.selectByCompanyId(mappingTaskInfo.getCompanyId());
                for (CompanyIpDetailsInfo companyIpDetailsInfo : companyIpDetailsInfoList) {
                    ipStrList.add(companyIpDetailsInfo.getIp());
                }
            }
            List<String> newIpStrList = new ArrayList<>();
            // 去重
            for (String s : ipStrList) {
                if (!newIpStrList.contains(s)) {
                    newIpStrList.add(s);
                }
            }
            if(newIpStrList.size() <= 0){
                ResponseModel responseModel = new ResponseModel();
                responseModel.error_code = -3;
                responseModel.error_message = "failed";
                message = JsonMapper.obj2String(responseModel);
                return message;
            }
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("task_id", taskId);
            paramsMap.put("ip_list", newIpStrList);
            paramsMap.put("portgrp_id", mappingTaskInfo.getPortsGroupId());
            PortGroupInfo portGroupInfo = portGroupInfoServiceImpl.selectByPrimaryKey(mappingTaskInfo.getPortsGroupId());
            if (portGroupInfo != null) {
                if (portGroupInfo.getIsPreset().equals("0")) {
                    // 自定义
                    paramsMap.put("portgrp_type", "1");
                    paramsMap.put("portgrp_list", portGroupInfo.getPortString().split(","));
                } else {
                    // 预置
                    paramsMap.put("portgrp_type", "0");
                    paramsMap.put("portgrp_list", "");
                }
            }
            if (null != taskUtilsConfig) {
                // 更新任务状态
                int num = setTaskStatusToScanning(mappingTaskInfo,userId);
                System.out.println("任务主键：" + taskId + ", 状态更新： " + (num > 0 ? "成功" : "失败"));
                if(num > 0) {
                    String requestStr = JsonMapper.obj2String(paramsMap);
                    System.out.println("下发指令：" + requestStr);
                    sendRequest.issue(paramsMap, "1", taskId);
                    ResponseModel responseModel = new ResponseModel();
                    responseModel.error_code = 0;
                    responseModel.error_message = "success";
                    System.out.println("下发结果：" + (responseModel.error_code == 0 ? "成功" : "失败"));
                    message = JsonMapper.obj2String(responseModel);
                }
            } else {
                System.out.println("taskUtilsConfig 注入失败！");
                logger.error("taskUtilsConfig 注入失败！");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage() + ex.toString());
            logger.error(ex.getMessage() + ex.toString());
            throw new RuntimeException("");
        } finally {
            lock.unlock();
        }
        return message;
    }

    /**
     * kafka 订阅方法
     */
    @Override
    public void TaskReceive() {

        kafkaConsumer = new KafkaConsumer(taskUtilsConfig.getKafkaAdr());
        // 添加回调方法
        kafkaConsumer.addListener(this, "processMessage",
                new ConsumerRecord<String, String>("", 0, 0, "", ""));
        // 订阅接收topic
        kafkaConsumer.consumer(taskUtilsConfig.getCyberspacemapTopic());
    }

    // 任务完成，处理全部返回数据方法
    public void handleMessage(String taskId) {
        // 处理网络空间测绘返回数据
        try {
            System.out.println("======================================= 空间测绘任务消息接收");
            receivelock.lock();
            List<DataPackageWithBLOBs> dataPackageWithBLOBsList = dataPackageServiceImpl.selectByTaskId(taskId);
            CyberspaceMapResponseModel cyberspaceMapResponseModel = new CyberspaceMapResponseModel();
            cyberspaceMapResponseModel.data = new ArrayList<>();
            if (dataPackageWithBLOBsList != null && dataPackageWithBLOBsList.size() > 0) {
                for (DataPackageWithBLOBs dataPackageWithBLOBs : dataPackageWithBLOBsList) {
                    if (!dataPackageWithBLOBs.getResponseString().equals("")) {
                        JSONObject jsonObject = JSONObject.parseObject(dataPackageWithBLOBs.getResponseString());
                        Map jsonToMap = JSONObject.parseObject(jsonObject.toJSONString());
                        CyberspaceMapResponseModel cyModel = JsonMapConvertToModelList(jsonToMap);
                        cyberspaceMapResponseModel.task_id = cyModel.task_id;
                        if(cyModel.data != null && cyModel.data.size() > 0){
                            cyberspaceMapResponseModel.data.addAll(cyModel.data);
                        }
                    }
                }
            } else {
                System.out.println("未查到返回数据包！");
                return;
            }
            List<String> exsitIpList = new ArrayList<>();
            List<Data> newIpDataList = new ArrayList<>();
            // ip 去重
            for (Data datum : cyberspaceMapResponseModel.data) {
                if (exsitIpList.indexOf(datum.ip) <= -1) {
                    exsitIpList.add(datum.ip);
                    newIpDataList.add(datum);
                }else {

                    System.out.println("本次扫描测绘任务 ID : " + cyberspaceMapResponseModel.task_id
                            + " ，ip : " + datum.ip + " 存在重复。具体内容请查看以下 JSON 串。");
                    System.out.println(JsonMapper.obj2String(datum));
                }
            }
            cyberspaceMapResponseModel.data = newIpDataList;
            // 查询空间测绘任务信息
            MappingTask mappingTaskInfo = getMappingTaskById(cyberspaceMapResponseModel.task_id);
            if (null != mappingTaskInfo) {
                // 判断任务状态
                if (!mappingTaskInfo.getTaskStatus().equals("1")) {
                    System.out.println("任务状态不在扫描中！task id : " + mappingTaskInfo.getId());
                    return;
                }
                int result = StepOne(cyberspaceMapResponseModel, mappingTaskInfo);
                if (result > 0) {
                    result = StepTwo(cyberspaceMapResponseModel, mappingTaskInfo);
                    if (result > 0) {
                        if (result > 0) {
                            // StepFour(cyberspaceMapResponseModel);
                        } else {
                            System.out.println("Step 3 执行失败！");
                        }
                    } else {
                        System.out.println("Step 2 执行失败！");
                    }
                } else {
                    System.out.println("Step 1 执行失败！");
                }
            } else {
                System.out.println("未查询到任务，任务 ID : " + cyberspaceMapResponseModel.task_id);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + ex.toString());
            logger.error(ex.getMessage() + ex.toString());
            throw new RuntimeException("");
        } finally {
            receivelock.unlock();
        }
    }


    // 处理"渗透工具"返回信息方法
    // api 接口返回方式，信息处理方法方法
    @Transactional(rollbackFor = Exception.class)
    public void processMessage(String msg) {
        // 处理网络空间测绘返回数据
        try {
            /**
             * 	Step 1 :
             *
             * 		更新 "空间测绘任务执行记录表"    // 变为更新
             * 		插入 "空间测绘任务执行记录关联IP表"
             * 		插入 "空间测绘任务执行记录关联端口表"
             * 		插入 "空间测绘任务执行记录端口规则关联表"
             *
             * 	Step 2 :
             *
             * 		IP/IP段 不需要查找 "空间测绘任务扫描对象表" 映射信息，直接入表
             * 		文件导入 需要查找 "空间测绘任务扫描对象表" 映射信息，匹配信息，入表
             *
             * 	Step 3 : // 删除该步骤，放在接收最后一个包是执行
             *
             * 		更新任务表 "扫描完成次数"
             * 		更新 "上次扫描时间"，"上次扫描记录ID"
             * 		更新任务状态为 "扫描完成"
             * 		清空本次扫描时间
             *
             * 	Step 4 :
             *
             * 	    更新 "t_mapping_obj_info" 关联资产IP的id（资产IP表id）
             *
             *  Step 5 :
             *
             *      更新 "t_temp_task" 表字段 complete_count ， current_package_no 和 update_time
             *      更新 "t_data_package" 表字段 response_string 和 update_time
             *
             * 	Step 6 : // 删除该步骤，放在接收最后一个包是执行
             *
             * 		判断任务表 "资产测绘完成后，立即执行漏洞渗透扫描" 状态
             * 		是 : 《渗透扫描任务操作步骤》
             * */
            receivelock.lock();
            System.out.println("======================================= 空间测绘任务消息接收");
            if (null != msg && !msg.equals("")) {
                String msgValue = msg;
                System.out.println("接收消息内容：" + msgValue);
                JSONObject jsonObject = JSONObject.parseObject(msgValue);
                Map jsonToMap = JSONObject.parseObject(jsonObject.toJSONString());
                CyberspaceMapResponseModel cyberspaceMapResponseModel = JsonMapConvertToModelList(jsonToMap);
                StepFive(cyberspaceMapResponseModel, msgValue);
                List<String> taskIdList = new ArrayList<>();
                taskIdList.add(cyberspaceMapResponseModel.task_id);
                sendRequest.execute(taskIdList);
            } else {
                System.out.println("未接收到消息数据！");
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + ex.toString());
            logger.error(ex.getMessage() + ex.toString());
            throw new RuntimeException("");
        } finally {
            receivelock.unlock();
        }
    }

    // 处理"渗透工具"返回信息方法
    // kafka 返回方式，信息处理方法方法
    public void processMessage(ConsumerRecord<String, String> msg) {
        if (null != msg && null != msg.value() && !msg.value().equals("")) {
            processMessage(msg.value());
        }
    }

    /**
     * Step 1 :
     *
     * 插入 "空间测绘任务执行记录表"
     * 插入 "空间测绘任务执行记录关联IP表"
     * 插入 "空间测绘任务执行记录关联端口表"
     * 插入 "空间测绘任务执行记录端口规则关联表"
     *
     * @param cyberspaceMapResponseModel 渗透工具返回数据对象
     * @param mappingTaskInfo            空间测绘任务信息
     * @return 1 成功 ; 0 失败
     */
    private int StepOne(CyberspaceMapResponseModel cyberspaceMapResponseModel, MappingTask mappingTaskInfo) {
        System.out.println("------------ Step 1 : a.插入 “空间测绘任务执行记录表”;b.插入 “空间测绘任务执行记录关联IP表”;c.插入 “空间测绘任务执行记录关联端口表”;d.插入 “空间测绘任务执行记录端口规则关联表”");
        int code = 0;
        try {
            // 更新 "空间测绘任务执行记录表"
            MappingTaskRecord mappingTaskRecordInfo = setMappingTaskRecordInfo(cyberspaceMapResponseModel, mappingTaskInfo);
            //mappingTaskInfo.setLastMappingTaskRecordId(mappingTaskRecordInfo.getId());
            MappingTaskRecord mappingTaskRecordCheckExsit = mappingTaskRecordServiceImpl.selectByTaskId(cyberspaceMapResponseModel.task_id);
            int result = 0;
            if (mappingTaskRecordCheckExsit != null) {
                mappingTaskRecordInfo = setUpdateMappingTaskRecordInfo(mappingTaskRecordCheckExsit,mappingTaskRecordInfo);
                result = mappingTaskRecordServiceImpl.update(mappingTaskRecordInfo);
            } else {
                result = mappingTaskRecordServiceImpl.insert(mappingTaskRecordInfo);
            }
            if (result > 0) {
                System.out.println("插入 “空间测绘任务执行记录表” success！");
                // 插入 "空间测绘任务执行记录关联IP表"
                // 插入 "空间测绘任务执行记录关联端口表"
                // 插入 "空间测绘任务执行记录端口规则关联表"
                List<MappingTaskRecordIp> mappingTaskRecordIpList = new ArrayList<>();
                List<MappingTaskRecordPort> mappingTaskRecordPortList = new ArrayList<>();
                List<MappingTaskRecordPortRule> mappingTaskRecordPortRuleList = new ArrayList<>();
                if (cyberspaceMapResponseModel.data != null) {
                    for (Data data : cyberspaceMapResponseModel.data) {
                        mappingTaskRecordIpList.add(setMappingTaskRecordIpInfo(mappingTaskInfo, data, mappingTaskRecordInfo.getId()));
                        if (data.ports == null) continue;
                        for (Ports ports : data.ports) {
                            mappingTaskRecordPortList.add(setMappingTaskRecordPortInfo(mappingTaskInfo, ports, data.ip, mappingTaskRecordInfo.getId()));
                            mappingTaskRecordPortRuleList.addAll(setMappingTaskRecordPortRuleInfo(mappingTaskInfo, ports.rules, data.ip, ports.port, mappingTaskRecordInfo.getId()));
                        }
                    }
                    if (mappingTaskRecordIpList.size() > 0)
                        mappingTaskRecordIpServiceImpl.insertBatch(mappingTaskRecordIpList);
                    if (mappingTaskRecordPortList.size() > 0)
                        mappingTaskRecordPortServiceImpl.insertBatch(mappingTaskRecordPortList);
                    if (mappingTaskRecordPortRuleList.size() > 0)
                        mappingTaskRecordPortRuleServiceImpl.insertBatch(mappingTaskRecordPortRuleList);
                }
                code = 1;
            } else {
                System.out.println("#插入 “空间测绘任务执行记录表” failed！");
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + ex.toString());
            logger.error(ex.getMessage() + ex.toString());
            throw new RuntimeException("");
        }
        return code;
    }

    /**
     * Step 2 :
     * <p>
     * IP/IP段 不需要查找 "空间测绘任务扫描对象表" 映射信息，直接入表
     * 文件导入 需要查找 "空间测绘任务扫描对象表" 映射信息，匹配信息，入表
     *
     * @param cyberspaceMapResponseModel 渗透工具返回数据对象
     * @param mappingTaskInfo            空间测绘任务信息
     * @return 1 成功 ; 0 失败
     */
    private int StepTwo(CyberspaceMapResponseModel cyberspaceMapResponseModel, MappingTask mappingTaskInfo) {
        System.out.println("------------ Step 2 : a.IP/IP段 不需要查找 ”空间测绘任务扫描对象表“ 映射信息，直接入表;b.文件导入 需要查找 “空间测绘任务扫描对象表” 映射信息，匹配信息，入表;");
        int code = 0;
        try {
            System.out.println("检查导入方式 : ");
            if(mappingTaskInfo.getScanObjType().equals("0")){
                // 扫描对象——单位全网段：返回结果，先将该单位关联的“资产IP表”、“资产端口表”
                // 全资产设为离线（“资产端口规则关联表”物理删除），
                // 再新增(新增的都是未备案)或更新相关IP和端口信息，新增端口指纹信息；
                System.out.println("单位全网段");
                StepTwoAllNetScan(cyberspaceMapResponseModel,mappingTaskInfo);
            }else{
                // 扫描对象——指定IP/IP段、指定资产：该单位关联的“资产IP表”、“资产端口表”、“资产端口规则关联表”不变，
                // 只做新增(新增的都是未备案)或更新相关IP和端口信息，新增端口指纹信息；
                System.out.println("IP/IP段/指定资产");
                StepTwoNotAllNetScan(cyberspaceMapResponseModel,mappingTaskInfo);
            }
            code = 1;
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + ex.toString());
            logger.error(ex.getMessage() + ex.toString());
            throw new RuntimeException("");
        }
        return code;
    }

    /**
     * Step 3 :
     * <p>
     * 更新任务表 "扫描完成次数"
     * 更新 "上次扫描时间"，"上次扫描记录ID"
     * 更新 "任务状态" 为 "未执行"
     * 清空 "本次扫描开始时间"
     *
     * @param taskId 任务主键
     * @return 1 成功 ; 0 失败
     */
    private int StepThree(String taskId,String lastMappingTaskRecordId) {
        int code = 0;
        // 查询任务信息
        MappingTask mappingTaskInfo = getMappingTaskById(taskId);
        if (mappingTaskInfo != null) {
            // 更新 "扫描完成次数"
            mappingTaskInfo.setTotalCount(mappingTaskInfo.getTotalCount() + 1);
            // 更新 "上次扫描时间"，"上次扫描记录ID"
            mappingTaskInfo.setLastScanTime(mappingTaskInfo.getNowScanTime());

            mappingTaskInfo.setLastMappingTaskRecordId(lastMappingTaskRecordId);
            // 更新 "任务状态" 为 "执行成功"
            mappingTaskInfo.setTaskStatus("4");
            // 清空 "本次扫描开始时间"
            mappingTaskInfo.setNowScanTime(null);
            mappingTaskInfo.setNowScanUserId(null);
            mappingTaskInfo.setUpdateTime(new Date());
            code = mappingTaskServiceImpl.updateByPrimaryKey(mappingTaskInfo);
        } else {
            System.out.println("#未查询到任务信息，任务ID : " + taskId);
        }
        return code;
    }

    /**
     * Step 4 :
     *
     * 更新 "t_mapping_obj_info" 关联资产IP的id（资产IP表id）
     *
     * */
    private int StepFour(CyberspaceMapResponseModel cyberspaceMapResponseModel) {
        int result = 1;
        List<MappingObjInfo> mappingObjInfoList = mappingObjInfoServiceImpl.selectByTaskId(cyberspaceMapResponseModel.task_id);
        List<MappingObjInfo> ipMappingObjList = new ArrayList<>();
        Map<String, String> ipMap = new HashMap<>();
        for (Data datum : cyberspaceMapResponseModel.data) {
            if (!ipMap.containsKey(datum.ip)) {
                ipMap.put(datum.ip, datum.id);
            }
        }
        /*for (MappingObjInfo mappingObjInfo : mappingObjInfoList) {
            if (mappingObjInfo.getObjType() != null &&
                    mappingObjInfo.getObjType().equals("0")) {
                if (mappingObjInfo.getIpInfoId() == null || mappingObjInfo.getIpInfoId().equals("")) {
                    if (ipMap.containsKey(mappingObjInfo.getObjValue())) {
                        mappingObjInfo.setIpInfoId(ipMap.get(mappingObjInfo.getObjValue()));
                        ipMappingObjList.add(mappingObjInfo);
                    }
                }
            }
        }*/
        if (ipMappingObjList.size() > 0) {
            result = mappingObjInfoServiceImpl.updateIpInfoIdByBatch(ipMappingObjList);
        }
        return result;
    }

    /**
     *
     *  Step 5 :
     *
     *      更新 "t_temp_task" 表字段 complete_count 和 update_time
     *      更新 "t_data_package" 表字段 response_string 和 update_time
     *
     * */
    private int StepFive(CyberspaceMapResponseModel cyberspaceMapResponseModel,String jsonStr) {
        int result = 0;
        List<String> taskIds = new ArrayList<>();
        taskIds.add(cyberspaceMapResponseModel.task_id);
        List<TempTask> tempTaskList = tempTaskServiceImpl.selectByTaskId(taskIds);
        if (tempTaskList != null && tempTaskList.size() > 0) {
            DataPackageWithBLOBs dataPackage = new DataPackageWithBLOBs();
            dataPackage.setTempTaskId(tempTaskList.get(0).getId());
            dataPackage.setTaskId(cyberspaceMapResponseModel.task_id);
            dataPackage.setPackageNo(cyberspaceMapResponseModel.package_no);
            DataPackageWithBLOBs dataPackageWithBLOBs = dataPackageServiceImpl.selectByCondition(dataPackage);
            if (dataPackageWithBLOBs != null) {
                TempTask tempTaskInfo = tempTaskList.get(0);
                TempTask updateTempTaskInfo = new TempTask();
                updateTempTaskInfo.setCompleteCount(tempTaskInfo.getCompleteCount() + 1);
                updateTempTaskInfo.setUpdateTime(new Date());
                updateTempTaskInfo.setId(tempTaskInfo.getId());
                dataPackageWithBLOBs.setResponseString(jsonStr);
                dataPackageWithBLOBs.setUpdateTime(new Date());
                tempTaskServiceImpl.updateByPrimaryKeySelective(updateTempTaskInfo);
                dataPackageServiceImpl.updateByPrimaryKeyWithBLOBs(dataPackageWithBLOBs);
            } else {
                System.out.println("未找到 t_data_package 信息！taskId : " + cyberspaceMapResponseModel.task_id +
                        " package_no : " + cyberspaceMapResponseModel.package_no +
                        " tempId : " + tempTaskList.get(0).getId());
            }
        } else {
            System.out.println("未找到 t_temp_task 信息");
        }
        return result;
    }

    /**
     * Step 6 :
     *
     * 判断任务表 "资产测绘完成后，立即执行漏洞渗透扫描" 状态
     * 		是 : 《渗透扫描任务操作步骤》
     * */
    private void StepSix(MappingTask mappingTask) {
        if (mappingTask.getScanningFlag().equals("1")) {
            penetrationScan.TaskPush(mappingTask.getScanningTaskId(),mappingTask.getNowScanUserId());
        }
    }

    /**
     * Last Step
     * */
    public void LastStep(String taskId) {
        // 更新记录表任务结束时间
        MappingTaskRecord mappingTaskRecordInfo = mappingTaskRecordServiceImpl.selectByTaskId(taskId);
        if (mappingTaskRecordInfo != null) {
            StepThree(taskId, mappingTaskRecordInfo.getId());
            mappingTaskRecordInfo.setEndTime(new Date());
            mappingTaskRecordServiceImpl.update(mappingTaskRecordInfo);
        }
        // 删除中间表信息
        tempTaskServiceImpl.deleteByTaskId(taskId);
        dataPackageServiceImpl.deleteByTaskId(taskId);
        MappingTask mappingTask = mappingTaskServiceImpl.selectByPrimaryKey(taskId);
        if (mappingTask != null) {
            StepSix(mappingTask);
        }
    }

    /**
     * Step 2 IP/IP段方式/指定资产
     *
     * @param cyberspaceMapResponseModel 渗透工具返回数据类型
     * @param mappingTaskInfo 空间测绘任务信息回想
     * 扫描对象——指定IP/IP段、指定资产：该单位关联的“资产IP表”、“资产端口表”、“资产端口规则关联表”不变，
     * 只做新增(新增的都是未备案)或更新相关IP和端口信息，新增端口指纹信息；
     * */
    private void StepTwoNotAllNetScan(CyberspaceMapResponseModel cyberspaceMapResponseModel,MappingTask mappingTaskInfo) {
        if (cyberspaceMapResponseModel.data == null) {
            return;
        }
        List<IpInfo> ipInfoList = ipInfoServiceImpl.selectByCompanyId(mappingTaskInfo.getCompanyId());
        Map<String, List<PortInfo>> portInfoMap = getPortInfoMapByCompanyId(mappingTaskInfo.getCompanyId());
        List<IpInfo> addIpInfoList = new ArrayList<>();
        List<IpInfo> updateIpInfoList = new ArrayList<>();
        List<PortInfo> addPortInfoList = new ArrayList<>();
        List<PortInfo> updatePortInfoList = new ArrayList<>();
        List<PortRule> addPortRuleList = new ArrayList<>();
        List<String> deletePortRuleByPortIdList = new ArrayList<>();
        Map<String, IpInfo> ipInfoMap = new HashMap<>();
        for (IpInfo ipInfo : ipInfoList) {
            ipInfoMap.put(ipInfo.getIp(), ipInfo);
        }
        for (Data data : cyberspaceMapResponseModel.data) {
            IpInfo ipInfo = null;
            if (ipInfoMap.containsKey(data.ip)) {
                // 修改
                ipInfo = setUpdateIpInfo(ipInfoMap.get(data.ip), data, mappingTaskInfo);
                updateIpInfoList.add(ipInfo);
                data.id = ipInfo.getId();   // 方便第四步，进行 t_mapping_obj_info 表维护
            } else {
                // 新增
                ipInfo = setAddIpInfo(data, mappingTaskInfo);
                addIpInfoList.add(ipInfo);
                data.id = ipInfo.getId(); // 方便第四步，进行 t_mapping_obj_info 表维护
            }
            PortAndPortRule portAndPortRule = StepTwoUpdatePort(data.ports, ipInfo, portInfoMap);
            addPortInfoList.addAll(portAndPortRule.addPortInfoListObj);
            updatePortInfoList.addAll(portAndPortRule.updatePortInfoListObj);
            addPortRuleList.addAll(portAndPortRule.addPortRuleListObj);
        }
        if(addIpInfoList.size() > 0)
            ipInfoServiceImpl.insertBatch(addIpInfoList);
        if(updateIpInfoList.size() > 0)
            ipInfoServiceImpl.updateBatch(updateIpInfoList);
        if(addPortInfoList.size() > 0)
            portInfoServiceImpl.insertBatch(addPortInfoList);
        if(updatePortInfoList.size() > 0)
            portInfoServiceImpl.updateBatch(updatePortInfoList);
        if(addPortRuleList.size() > 0)
            portRuleServiceImpl.insertBatch(addPortRuleList);
    }

    /**
     * Step 2 单位全网段扫描
     *
     * @param cyberspaceMapResponseModel 渗透工具返回数据类型
     * @param mappingTaskInfo 空间测绘任务信息回想
     * 扫描对象——单位全网段：返回结果，先将该单位关联的“资产IP表”、“资产端口表”
     * 全资产设为离线（“资产端口规则关联表”物理删除），
     * 再新增(新增的都是未备案)或更新相关IP和端口信息，新增端口指纹信息；
     * */
    private void StepTwoAllNetScan(CyberspaceMapResponseModel cyberspaceMapResponseModel,MappingTask mappingTaskInfo){
        if (cyberspaceMapResponseModel.data == null) {
            return;
        }
        List<IpInfo> ipInfoList = ipInfoServiceImpl.selectByCompanyId(mappingTaskInfo.getCompanyId());
        Map<String, List<PortInfo>> portInfoMap = getPortInfoMapByCompanyId(mappingTaskInfo.getCompanyId());
        List<IpInfo> addIpInfoList = new ArrayList<>();
        List<IpInfo> updateIpInfoList = new ArrayList<>();
        List<PortInfo> addPortInfoList = new ArrayList<>();
        List<PortInfo> updatePortInfoList = new ArrayList<>();
        List<PortRule> addPortRuleList = new ArrayList<>();
        List<String> deletePortRuleByPortIdList = new ArrayList<>();
        Map<String, IpInfo> ipInfoMap = new HashMap<>();
        for (IpInfo ipInfo : ipInfoList) {
            ipInfoMap.put(ipInfo.getIp(), ipInfo);
        }
        for (Data data : cyberspaceMapResponseModel.data) {
            IpInfo ipInfo = null;
            if (ipInfoMap.containsKey(data.ip)) {
                // 修改
                ipInfo = setUpdateIpInfo(ipInfoMap.get(data.ip), data, mappingTaskInfo);
                updateIpInfoList.add(ipInfo);
                data.id = ipInfo.getId();   // 方便第四步，进行 t_mapping_obj_info 表维护
            } else {
                // 新增
                ipInfo = setAddIpInfo(data, mappingTaskInfo);
                addIpInfoList.add(ipInfo);
                data.id = ipInfo.getId(); // 方便第四步，进行 t_mapping_obj_info 表维护
            }
            PortAndPortRule portAndPortRule = StepTwoUpdatePort(data.ports, ipInfo, portInfoMap);
            addPortInfoList.addAll(portAndPortRule.addPortInfoListObj);
            updatePortInfoList.addAll(portAndPortRule.updatePortInfoListObj);
            addPortRuleList.addAll(portAndPortRule.addPortRuleListObj);
            deletePortRuleByPortIdList.addAll(portAndPortRule.deletePortRuleByPortIdListObj);
        }
        if (cyberspaceMapResponseModel.data.size() > 0)
            ipInfoServiceImpl.offlineByCompanyId(mappingTaskInfo.getCompanyId());
        if(addIpInfoList.size() > 0)
            ipInfoServiceImpl.insertBatch(addIpInfoList);
        if(updateIpInfoList.size() > 0)
            ipInfoServiceImpl.updateBatch(updateIpInfoList);
        if (cyberspaceMapResponseModel.data.size() > 0)
            portInfoServiceImpl.offlineByCompanyId(mappingTaskInfo.getCompanyId());
        if(addPortInfoList.size() > 0)
            portInfoServiceImpl.insertBatch(addPortInfoList);
        if(updatePortInfoList.size() > 0)
            portInfoServiceImpl.updateBatch(updatePortInfoList);
        if(deletePortRuleByPortIdList.size() > 0)
            portRuleServiceImpl.deleteByPortId(deletePortRuleByPortIdList);
        if(addPortRuleList.size() > 0)
            portRuleServiceImpl.insertBatch(addPortRuleList);
    }

    /**
     * Step 2 更新 port
     * */
    private PortAndPortRule StepTwoUpdatePort(List<Ports> ports,IpInfo ipInfo,Map<String,List<PortInfo>> portInfoMap) {
        List<PortInfo> addPortInfoList = new ArrayList<>();
        List<PortInfo> updatePortInfoList = new ArrayList<>();
        List<String> deletePortRuleByPortIdList = new ArrayList<>();
        List<PortRule> addPortRuleList = new ArrayList<>();
        Map<Integer, PortInfo> portMap = new HashMap<>();
        if (portInfoMap.containsKey(ipInfo.getId())) {
            List<PortInfo> portInfoList = portInfoMap.get(ipInfo.getId());
            portInfoList.forEach(portInfo -> {
                portMap.put(portInfo.getPort(), portInfo);
            });
        }
        if(ports != null) {
            for (Ports port : ports) {
                PortInfo portInfo = new PortInfo();
                portInfo.setCompanyId(ipInfo.getCompanyId());
                portInfo.setAssetId(ipInfo.getAssetId());
                portInfo.setAssetIpId(ipInfo.getId());
                portInfo.setPort(port.port);
                portInfo.setPortStatus("1");
                portInfo.setService(port.service);
                portInfo.setProduct(port.product);
                portInfo.setProductVersion(port.product_version);
                portInfo.setProtocol(port.protocol);
                portInfo.setExtrainfo(port.extrainfo);
                portInfo.setMidware(port.midware);
                portInfo.setApp(port.app);
                portInfo.setCms(port.cms);
                portInfo.setTitle(port.title);
                portInfo.setRules(port.rules);
                portInfo.setUpdateTime(new Date());
                portInfo.setIsDelete("0");
                portInfo.setIsSuggestOff(port.isSuggestOff);
                log.info("封装portInfo完毕，详细：{}", portInfo.toString());
                if (portMap.containsKey(port.port)) {
                    // 更新
                    PortInfo myPort = portMap.get(port.port);
                    portInfo.setId(myPort.getId());
                    portInfo.setCreateTime(myPort.getCreateTime());
                    updatePortInfoList.add(portInfo);
                    deletePortRuleByPortIdList.add(myPort.getId());
                } else {
                    // 新增
                    portInfo.setPortRegStatus("0"); // 未备案的
                    portInfo.setId(UuidUtil.getUuid());
                    portInfo.setCreateTime(new Date());
                    addPortInfoList.add(portInfo);
                }
                addPortRuleList.addAll(StepTwoUpdatePortRule(portInfo, port.rules));
            }
        }
        return new PortAndPortRule() {{
            addPortInfoListObj = addPortInfoList;
            addPortRuleListObj = addPortRuleList;
            deletePortRuleByPortIdListObj = deletePortRuleByPortIdList;
            updatePortInfoListObj = updatePortInfoList;
        }};
    }

    /**
     * Step 2 更新 port rule
     * */
    private List<PortRule> StepTwoUpdatePortRule(PortInfo portInfo,String rule) {
        List<PortRule> portRuleList = new ArrayList<>();
        if (rule != null && !rule.equals("")) {
            String[] rules = rule.split(",");
            for (int i = 0; i < rules.length; i++) {
                PortRule portRule = new PortRule();
                portRule.setId(UuidUtil.getUuid());
                portRule.setCompanyId(portInfo.getCompanyId());
                portRule.setAssetId(portInfo.getAssetId());
                portRule.setAssetIpId(portInfo.getAssetIpId());
                portRule.setAssetPortId(portInfo.getId());
                portRule.setRuleId(Integer.parseInt(rules[i]));
                portRule.setCreateTime(new Date());
                portRuleList.add(portRule);
            }
        }
        return portRuleList;
    }

    /**
     * 根据任务主键获取网络空间任务信息
     */
    private MappingTask getMappingTaskById(String taskId) {
        MappingTask mappingTask = mappingTaskServiceImpl.selectByPrimaryKey(taskId);
        return mappingTask;
    }

    /**
     * 根据单位主键获取port信息
     *
     * @param companyId 单位主键
     * @return port信息 map
     * */
    private Map<String,List<PortInfo>> getPortInfoMapByCompanyId(String companyId) {
        Map<String, List<PortInfo>> portInfoMap = new HashMap<>();
        List<PortInfo> portInfoList = portInfoServiceImpl.selectByCompanyId(companyId);
        for (PortInfo portInfo : portInfoList) {
            if (portInfoMap.containsKey(portInfo.getAssetIpId())) {
                portInfoMap.get(portInfo.getAssetIpId()).add(portInfo);
            } else {
                portInfoMap.put(portInfo.getAssetIpId(), new ArrayList<PortInfo>() {{
                    add(portInfo);
                }});
            }
        }
        return portInfoMap;
    }

    /**
     * 设置空间测绘任务执行记录信息
     *
     * @param cyberspaceMapResponseModel 渗透工具返回数据对象
     * @param mappingTaskInfo            空间测绘任务信息对象
     * @return 空间测绘任务执行记录信息对象
     */
    private MappingTaskRecord setMappingTaskRecordInfo(CyberspaceMapResponseModel cyberspaceMapResponseModel, MappingTask mappingTaskInfo) {
        MappingTaskRecord mappingTaskRecordInfo = new MappingTaskRecord();
        mappingTaskRecordInfo.setId(UuidUtil.getUuid());
        mappingTaskRecordInfo.setCompanyId(mappingTaskInfo.getCompanyId());
        mappingTaskRecordInfo.setMappingTaskId(cyberspaceMapResponseModel.task_id);
        if (cyberspaceMapResponseModel.data != null) {
            mappingTaskRecordInfo.setLiveIpCount(cyberspaceMapResponseModel.data.size());
            int openPortCount = cyberspaceMapResponseModel.data.stream().mapToInt(data -> (data.ports != null ? data.ports.size() : 0)).sum();
            mappingTaskRecordInfo.setOpenPortCount(openPortCount);
        } else {
            mappingTaskRecordInfo.setLiveIpCount(0);
            mappingTaskRecordInfo.setOpenPortCount(0);
        }
        mappingTaskRecordInfo.setStartTime(mappingTaskInfo.getNowScanTime());
        //mappingTaskRecordInfo.setEndTime(new Date()); // 由于分包操作，所以结束时间在最后一个包接收时更新
        mappingTaskRecordInfo.setCreateUserId(mappingTaskInfo.getNowScanUserId());
        mappingTaskRecordInfo.setCreateTime(new Date());
        mappingTaskRecordInfo.setIsDelete("0");
        return mappingTaskRecordInfo;
    }

    /**
     * 设置更新空间测绘任务执行记录信息
     *
     * @param oldMappingTaskRecord  数据库中已存在的
     * @param newMappingTaskRecord  需要更新的内容
     * @return 空间测绘任务执行记录信息对象
     */
    private MappingTaskRecord setUpdateMappingTaskRecordInfo(MappingTaskRecord oldMappingTaskRecord,MappingTaskRecord newMappingTaskRecord) {
        newMappingTaskRecord.setId(oldMappingTaskRecord.getId());
        newMappingTaskRecord.setLiveIpCount(oldMappingTaskRecord.getLiveIpCount() + newMappingTaskRecord.getLiveIpCount());
        newMappingTaskRecord.setOpenPortCount(oldMappingTaskRecord.getOpenPortCount() + newMappingTaskRecord.getOpenPortCount());
        return newMappingTaskRecord;
    }
    /**
     * 设置空间测绘任务执行记录ip信息
     *
     * @param mappingTaskInfo     空间测绘任务信息对象
     * @param data                渗透工具返回ip信息对象
     * @param mappingTaskRecordId 空间测绘任务执行记录 ID
     * @return 空间测绘任务执行记录ip信息对象
     */
    private MappingTaskRecordIp setMappingTaskRecordIpInfo(MappingTask mappingTaskInfo, Data data, String mappingTaskRecordId) {
        MappingTaskRecordIp mappingTaskRecordIpInfo = new MappingTaskRecordIp();
        try {
            mappingTaskRecordIpInfo.setId(UuidUtil.getUuid());
            mappingTaskRecordIpInfo.setTaskRecordId(mappingTaskRecordId);
            mappingTaskRecordIpInfo.setCompanyId(mappingTaskInfo.getCompanyId());
            mappingTaskRecordIpInfo.setMappingTaskId(mappingTaskInfo.getId());
            mappingTaskRecordIpInfo.setIp(data.ip);
            mappingTaskRecordIpInfo.setSystem(data.system);
            mappingTaskRecordIpInfo.setSystemType(data.system_type);
            mappingTaskRecordIpInfo.setSystemVersion(data.system_version);
            mappingTaskRecordIpInfo.setVendor(data.vendor);
            mappingTaskRecordIpInfo.setCreateTime(new Date());
            mappingTaskRecordIpInfo.setMappingScantime(simpleDateFormat.parse(data.create_time));
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + ex.toString());
            logger.error(ex.getMessage() + ex.toString());
            throw new RuntimeException("");
        }
        return mappingTaskRecordIpInfo;
    }

    /**
     * 设置空间测绘任务执行记录port信息
     *
     * @param mappingTaskInfo     空间测绘任务信息对象
     * @param ports               渗透工具返回的port信息对象
     * @param ip                  ip
     * @param mappingTaskRecordId 空间测绘任务执行记录ID
     * @return 空间测绘任务执行记录port对象
     */
    private MappingTaskRecordPort setMappingTaskRecordPortInfo(MappingTask mappingTaskInfo, Ports ports, String ip, String mappingTaskRecordId) {
        MappingTaskRecordPort mappingTaskRecordPortInfo = new MappingTaskRecordPort();
        try {
            mappingTaskRecordPortInfo.setId(UuidUtil.getUuid());
            mappingTaskRecordPortInfo.setTaskRecordId(mappingTaskRecordId);
            mappingTaskRecordPortInfo.setCompanyId(mappingTaskInfo.getCompanyId());
            mappingTaskRecordPortInfo.setMappingTaskId(mappingTaskInfo.getId());
            mappingTaskRecordPortInfo.setPort(ports.port);
            mappingTaskRecordPortInfo.setIp(ip);
            mappingTaskRecordPortInfo.setService(ports.service);
            mappingTaskRecordPortInfo.setProduct(ports.product);
            mappingTaskRecordPortInfo.setProductVersion(ports.product_version);
            mappingTaskRecordPortInfo.setProtocol(ports.protocol);
            mappingTaskRecordPortInfo.setExtrainfo(ports.extrainfo);
            mappingTaskRecordPortInfo.setMidware(ports.midware);
            mappingTaskRecordPortInfo.setApp(ports.app);
            mappingTaskRecordPortInfo.setCms(ports.cms);
            mappingTaskRecordPortInfo.setTitle(ports.title);
            mappingTaskRecordPortInfo.setRules(ports.rules);
            mappingTaskRecordPortInfo.setCreateTime(simpleDateFormat.parse(ports.create_time));
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + ex.toString());
            logger.error(ex.getMessage() + ex.toString());
            throw new RuntimeException("");
        }
        return mappingTaskRecordPortInfo;
    }

    /**
     * 设置空间测绘任务执行记录port rule信息
     *
     * @param mappingTaskInfo     空间测绘任务信息对象
     * @param portRule            端口规则字符串
     * @param ip                  ip
     * @param port                端口
     * @param mappingTaskRecordId 空间测绘任务执行记录ID
     * @return 空间测绘任务执行记录port rule list
     */
    private List<MappingTaskRecordPortRule> setMappingTaskRecordPortRuleInfo(MappingTask mappingTaskInfo, String portRule, String ip, int port, String mappingTaskRecordId) {
        List<MappingTaskRecordPortRule> mappingTaskRecordPortRuleList = new ArrayList<>();
        try {
            if (portRule != null && !portRule.isEmpty()) {
                String[] portRuleArray = portRule.split(",");
                for (int i = 0; i < portRuleArray.length; i++) {
                    MappingTaskRecordPortRule mappingTaskRecordPortRuleInfo = new MappingTaskRecordPortRule();
                    mappingTaskRecordPortRuleInfo.setId(UuidUtil.getUuid());
                    mappingTaskRecordPortRuleInfo.setTaskRecordId(mappingTaskRecordId);
                    mappingTaskRecordPortRuleInfo.setCompanyId(mappingTaskInfo.getCompanyId());
                    mappingTaskRecordPortRuleInfo.setMappingTaskId(mappingTaskInfo.getId());
                    mappingTaskRecordPortRuleInfo.setIp(ip);
                    mappingTaskRecordPortRuleInfo.setPort(port);
                    mappingTaskRecordPortRuleInfo.setRuleId(Integer.parseInt(portRuleArray[i]));
                    mappingTaskRecordPortRuleInfo.setCreateTime(new Date());
                    mappingTaskRecordPortRuleList.add(mappingTaskRecordPortRuleInfo);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + ex.toString());
            logger.error(ex.getMessage() + ex.toString());
            throw new RuntimeException("");
        }
        return mappingTaskRecordPortRuleList;
    }

    /**
     * 设置ip信息
     * */
    private IpInfo setAddIpInfo(Data data,MappingTask mappingTaskInfo) {
        IpInfo ipInfo = new IpInfo();
        ipInfo.setId(UuidUtil.getUuid());
        ipInfo.setCompanyId(mappingTaskInfo.getCompanyId());
        ipInfo.setAssetId("");
        ipInfo.setIp(data.ip);
        ipInfo.setIpStatus("1");
        ipInfo.setIpRegStatus("0"); // 新增默认 "0",即未备案状态
        ipInfo.setSystem(data.system);
        ipInfo.setSystemType(data.system_type);
        ipInfo.setSystemVersion(data.system_version);
        ipInfo.setVendor(data.vendor);
        ipInfo.setCreateTime(new Date());
        ipInfo.setCreateUserId(mappingTaskInfo.getCreateUserId());
        ipInfo.setUpdateTime(new Date());
        ipInfo.setUpdateUserId(mappingTaskInfo.getUpdateUserId());
        ipInfo.setIsDelete("0");
        return ipInfo;
    }

    /**
     * 设置ip信息
     * */
    private IpInfo setUpdateIpInfo(IpInfo ipInfo,Data data,MappingTask mappingTaskInfo) {
        ipInfo.setIpStatus("1");
        ipInfo.setSystem(data.system);
        ipInfo.setSystemType(data.system_type);
        ipInfo.setSystemVersion(data.system_version);
        ipInfo.setVendor(data.vendor);
        ipInfo.setUpdateTime(new Date());
        ipInfo.setUpdateUserId(mappingTaskInfo.getUpdateUserId());
        return ipInfo;
    }

    /**
     * 更新 空间测绘任务 状态信息
     * */
    private int setTaskStatusToScanning(MappingTask mappingTaskInfo,String userId) {
        mappingTaskInfo.setTaskStatus("1");
        mappingTaskInfo.setUpdateTime(new Date());
        mappingTaskInfo.setNowScanTime(new Date());
        mappingTaskInfo.setNowScanUserId(userId);
        return mappingTaskServiceImpl.updateByPrimaryKey(mappingTaskInfo);
    }

    private CyberspaceMapResponseModel  JsonMapConvertToModelList(Map jsonMap) {
        CyberspaceMapResponseModel model = new CyberspaceMapResponseModel();
        if (jsonMap.containsKey("task_id")) {
            model.task_id = jsonMap.get("task_id").toString();
        }
        if(jsonMap.containsKey("package_no")) {
            model.package_no = Integer.parseInt(jsonMap.get("package_no").toString());
        }
        if (jsonMap.containsKey("data")) {
            model.data = new ArrayList<>();
            if (jsonMap.get("data") != null && !jsonMap.get("data").toString().equals("")) {
                JSONArray jsonBodyArray = (JSONArray) jsonMap.get("data");
                for (Object object : jsonBodyArray) {
                    Data data = new Data();
                    JSONObject jsonObject = (JSONObject) object;
                    log.info("老代码，此处可能存在相同数据，遍历2次，后期需优化：{}", jsonObject);
                    if (jsonObject.containsKey("ip")) data.ip = jsonObject.get("ip").toString();
                    if (jsonObject.containsKey("system")) data.system = jsonObject.get("system").toString();
                    if (jsonObject.containsKey("vendor")) data.vendor = jsonObject.get("vendor").toString();
                    if (jsonObject.containsKey("system_type"))
                        data.system_type = jsonObject.get("system_type").toString();
                    if (jsonObject.containsKey("create_time"))
                        data.create_time = jsonObject.get("create_time").toString();
                    if (jsonObject.containsKey("system_version"))
                        data.system_version = jsonObject.get("system_version").toString();
                    if (jsonObject.containsKey("ports")) {
                        data.ports = new ArrayList<>();
                        JSONArray jsonPortArray = (JSONArray) jsonObject.get("ports");
                        for (Object portObject : jsonPortArray) {
                            Ports port = new Ports();
                            JSONObject jsonPortObj = (JSONObject) portObject;
                            if (jsonPortObj.containsKey("port"))
                                port.port = Integer.parseInt(jsonPortObj.get("port").toString());
                            if (jsonPortObj.containsKey("product"))
                                port.product = jsonPortObj.get("product").toString();
                            if (jsonPortObj.containsKey("protocol"))
                                port.protocol = jsonPortObj.get("protocol").toString();
                            if (jsonPortObj.containsKey("version"))
                                port.product_version = jsonPortObj.get("version").toString();
                            if (jsonPortObj.containsKey("extrainfo"))
                                port.extrainfo = jsonPortObj.get("extrainfo").toString();
                            if (jsonPortObj.containsKey("cpe")) port.service = jsonPortObj.get("cpe").toString();
                            if (jsonPortObj.containsKey("app")) port.app = jsonPortObj.get("app").toString();
                            if (jsonPortObj.containsKey("title")) port.title = jsonPortObj.get("title").toString();
                            if (jsonPortObj.containsKey("rules")) port.rules = jsonPortObj.get("rules").toString();
                            if (jsonPortObj.containsKey("midware")) port.midware = jsonPortObj.get("midware").toString();
                            if (jsonPortObj.containsKey("cms")) port.cms = jsonPortObj.get("cms").toString();
                            if (jsonPortObj.containsKey("create_time"))
                                port.create_time = jsonPortObj.get("create_time").toString();

                            // 端口是否建议关闭，处理方法
                            portIsSuggestOff(port, jsonPortObj);

                            data.ports.add(port);
                        }
                    }
                    model.data.add(data);
                }
            }
        }
        log.info("model组装完毕，详情：{}", model);
        return model;
    }

    /**
     * 端口，是否建议关闭
     *
     * @param port        端口
     * @param jsonPortObj 数据参数
     */
    private Ports portIsSuggestOff(Ports port, JSONObject jsonPortObj) {
        log.info("portIsSuggestOff 开始执行！");
        List<DicBusiCodeVO> portSuggestParams = dicBusiService.getPortSuggestParams();

        // 是否检查title
        boolean isCheckTitle = jsonPortObj.containsKey(DicBusiEnum.BUSI_TITLE_CODE.getVal()) || jsonPortObj.containsKey(DicBusiEnum.BUSI_TITLE_CODE.getVal().toLowerCase()) ? true : false;

        // 是否检查body
        boolean isCheckBody = jsonPortObj.containsKey(DicBusiEnum.BUSI_BODY_CODE.getVal()) || jsonPortObj.containsKey(DicBusiEnum.BUSI_BODY_CODE.getVal().toLowerCase()) ? true : false;

        for (DicBusiCodeVO dic : portSuggestParams) {
            String dicBusiCode = dic.getBusiCode();
            String dicBusiValue = dic.getValue();

            // 端口建议数据中，有title数据，同时测绘任务返回值中，也包含title属性
            if (DicBusiEnum.BUSI_TITLE_CODE.getVal().equals(dicBusiCode) && isCheckTitle) {
                if (dicBusiValue.contains(jsonPortObj.get("title").toString())) {
                    log.info("port ：{} ，title 建议关闭！", port.port);
                    port.isSuggestOff = Integer.parseInt(DicBusiEnum.VALUE_IS_TRUE.getVal());
                    return port;
                }
            } else if (DicBusiEnum.BUSI_BODY_CODE.getVal().equals(dicBusiCode) && isCheckBody) {
                if (dicBusiValue.contains(jsonPortObj.get("body").toString())) {
                    log.info("port ：{} ，body 建议关闭！", port.port);
                    port.isSuggestOff = Integer.parseInt(DicBusiEnum.VALUE_IS_TRUE.getVal());
                    return port;
                }
            }

            // 建议关闭端口，与当前端口匹配，则建议关闭
            if (dic.getValue().equals(jsonPortObj.get("port").toString())) {
                log.info("port ：{} ，端口黑名单 建议关闭！", port.port);
                port.isSuggestOff = Integer.parseInt(DicBusiEnum.VALUE_IS_TRUE.getVal());
                return port;
            }
        }
        log.info("portIsSuggestOff 结束！");
        return port;
    }

    // 检查消息是否处理过
    public boolean checkMsgIsHandle(String taskId,int packageNo) {
        boolean result = false;
        try {
            DataPackageWithBLOBs dataPackageWithBLOBs = new DataPackageWithBLOBs();
            dataPackageWithBLOBs.setTaskId(taskId);
            dataPackageWithBLOBs.setPackageNo(packageNo);
            DataPackageWithBLOBs dataPackageInfo = dataPackageServiceImpl.selectByCondition(dataPackageWithBLOBs);
            if (dataPackageInfo != null &&
                    dataPackageInfo.getResponseString() != null &&
                    !dataPackageInfo.getResponseString().equals("")) {
                result = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * 最后更新
     * */


    class PortAndPortRule {

        List<PortInfo> addPortInfoListObj;

        List<PortInfo> updatePortInfoListObj;

        List<String> deletePortRuleByPortIdListObj;

        List<PortRule> addPortRuleListObj;
    }

    /**
     * 测试入口
     */
    public static void main(String[] args) {
        //boolean success = new CyberspaceMap().TaskPush("");
        //new CyberspaceMap().processMessage("");
        //String[] ips = IpUtils.IpSegParsing("10.10.1.00-10.10.1.255");

        String val = "192.168.0.1-192.168.0.10";
        List<String> ipStrList = new ArrayList<>();
        // IP段
        if (val.indexOf("-") > -1) {
            // ip段 192.168.0.1 - 192.168.0.19
            ipStrList.addAll(new ArrayList(Arrays.asList(IpUtils.IpSegParsing(val))));
        } else if (val.indexOf("/") > -1) {
            // cidr 192.168.0.1/18
            ipStrList.addAll(Arrays.asList(IpUtils.CrdiParsing(val)));
        }
        val = "192.168.2.10";
        // IP
        if (!ipStrList.contains(val)) {
            ipStrList.add(val);
        }


        try {
            String msgValue = "{\"data\":null,\"task_id\":\"aca313e4b5984f138b9526705a069d4c\"}";
            JSONObject jsonObject = JSONObject.parseObject(msgValue);
            Map jsonToMap = JSONObject.parseObject(jsonObject.toJSONString());
            new CyberspaceMap().JsonMapConvertToModelList(jsonToMap);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getTaskStatus(String json) {
        String code = "";
        if(!json.equals("")) {
            JSONObject jsonObject = JSONObject.parseObject(json);
            Map jsonToMap = JSONObject.parseObject(jsonObject.toJSONString());
            if (jsonToMap.containsKey("data")) {
                if (!jsonToMap.get("data").equals("")) {
                    JSONObject status = (JSONObject) jsonToMap.get("data");
                    if (status.containsKey("status")) {
                        code = status.get("status").toString();
                    }
                }
            }
        }
        return code;
    }
}
