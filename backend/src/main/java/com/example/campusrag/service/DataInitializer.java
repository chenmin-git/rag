package com.example.campusrag.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.campusrag.domain.Feedback;
import com.example.campusrag.domain.SystemConfig;
import com.example.campusrag.domain.OperationLog;
import com.example.campusrag.domain.QaRecord;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.config.AppProperties;
import com.example.campusrag.repository.FeedbackRepository;
import com.example.campusrag.repository.KnowledgeDocumentRepository;
import com.example.campusrag.repository.OperationLogRepository;
import com.example.campusrag.repository.QaRecordRepository;
import com.example.campusrag.repository.SystemConfigRepository;
import com.example.campusrag.repository.UserAccountRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserAccountRepository userRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final SystemConfigRepository configRepository;
    private final QaRecordRepository qaRecordRepository;
    private final FeedbackRepository feedbackRepository;
    private final OperationLogRepository logRepository;
    private final DocumentService documentService;
    private final AppProperties properties;

    public DataInitializer(
            UserAccountRepository userRepository,
            KnowledgeDocumentRepository documentRepository,
            SystemConfigRepository configRepository,
            QaRecordRepository qaRecordRepository,
            FeedbackRepository feedbackRepository,
            OperationLogRepository logRepository,
            DocumentService documentService,
            AppProperties properties) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.configRepository = configRepository;
        this.qaRecordRepository = qaRecordRepository;
        this.feedbackRepository = feedbackRepository;
        this.logRepository = logRepository;
        this.documentService = documentService;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedConfigs();
        seedDocuments();
        seedQaAndFeedback();
        seedLogs();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.saveAll(List.of(
                user("admin", "系统管理员", "SYSTEM_ADMIN", "信息化办公室"),
                user("teacher", "张老师", "TEACHER", "教务处"),
                user("student", "李同学", "STUDENT", "学生工作处"),
                user("dept", "院系管理员", "DEPARTMENT_ADMIN", "学生工作处"),
                user("teacher_li", "李老师", "TEACHER", "图书馆"),
                user("student_wang", "王同学", "STUDENT", "后勤服务中心")));
    }

    private UserAccount user(String username, String displayName, String role, String department) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash("123456");
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }

    private void seedConfigs() {
        AppProperties.Spark spark = properties.getSpark();
        String apiPassword = firstNonBlank(
                spark.getApiPassword(),
                System.getenv("SPARK_API_PASSWORD"),
                System.getenv("XFYUN_SPARK_API_PASSWORD"));
        boolean sparkEnabled = spark.isEnabled() || !apiPassword.isBlank();
        Map<String, String> defaults = Map.ofEntries(
                entry("rag.topK", "5"),
                entry("rag.similarityThreshold", "0.20"),
                entry("rag.maxContextChars", "5200"),
                entry("spark.enabled", String.valueOf(sparkEnabled)),
                entry("spark.provider", spark.getProvider()),
                entry("spark.protocol", spark.getProtocol()),
                entry("spark.endpoint", spark.getEndpoint()),
                entry("spark.model", spark.getModel()),
                entry("spark.apiPassword", apiPassword),
                entry("spark.temperature", String.valueOf(spark.getTemperature())),
                entry("spark.maxTokens", String.valueOf(spark.getMaxTokens())),
                entry("milvus.collection", "campus_knowledge_chunks"),
                entry("embedding.dimension", "384"));
        defaults.forEach((key, value) -> configRepository.findByConfigKey(key).orElseGet(() -> {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription("系统默认参数");
            return configRepository.save(config);
        }));
    }

    private Entry<String, String> entry(String key, String value) {
        return Map.entry(key, value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private void seedDocuments() {
        if (documentRepository.count() > 0) {
            return;
        }
        UserAccount admin = userRepository.findByUsername("admin").orElseThrow();
        documentService.ingestText("学生手册-综合服务规则.md", """
                学生请假应通过线上办事大厅提交申请。病假需上传校医院或二级甲等以上医院证明，事假需说明具体原因并经辅导员审核。三天以内由辅导员审批，三天以上七天以内由学院审批，七天以上需报学生工作处备案。

                请假审批通过后，学生应主动告知任课教师并按课程要求补交作业。未经批准擅自离校或未按期返校的，学院可按学生日常管理规定进行记录和提醒。

                奖学金评定一般在每学年第一学期开展。参评学生应无未解除的纪律处分，上一学年课程无不及格记录，综合测评成绩位于专业前列。国家奖学金、励志奖学金和校级奖学金不能在同一学年重复获得同类资助。

                困难生认定由学生本人申请、班级评议、学院审核和学校备案组成。申请材料应真实完整，家庭经济情况发生明显变化时可以申请动态调整。

                校园卡遗失后，学生可在校园卡服务中心窗口或线上办事大厅办理挂失。补办校园卡需携带本人有效证件，工作日受理后通常当天可领取新卡。原卡余额会在补卡完成后自动转入新卡。

                校园卡异常消费、无法刷卡或余额显示异常时，可先在自助终端查询交易记录，再到校园卡服务中心登记处理。涉及补卡后余额未转入的，应提供本人证件和补卡记录。

                学生宿舍报修可通过后勤服务平台提交。紧急维修包括漏水、断电、门锁损坏等情况，后勤部门将在接单后优先处理。普通维修原则上在三个工作日内完成。

                宿舍晚归、外宿和大功率电器使用按照宿舍管理规定处理。学生应保持宿舍公共区域整洁，发现安全隐患应及时向宿管或辅导员报告。
                """, "学生工作处", "STUDENT", admin);
        documentService.ingestText("教务管理-课程与考试指南.md", """
                课程退选应在学校规定的选课调整周内完成。超过退选期限后，学生原则上不能退选课程，确因培养方案调整、身体原因等特殊情况需要退选的，应提交学院和教务处审核。

                选课期间学生应确认培养方案、课程容量、上课时间和考试安排。重复选课、冲突选课或未按要求完成确认造成的后果由学生本人承担。

                期末考试缓考申请应在考试前提交。因病申请缓考须提供医疗证明，因公或其他不可抗力原因申请缓考须提供相应证明材料。未经批准缺考的课程成绩按学校考试管理规定处理。

                缓考获批后，学生应关注教务系统发布的补考或缓考安排。缓考成绩按实际考试成绩记载，未按时参加缓考的按相关考试管理规定处理。

                成绩复核由学生本人在成绩公布后规定时间内提出申请。复核范围主要包括漏评、错登、总分统计错误等，不重新评阅试卷主观题评分尺度。

                成绩复核结果由开课单位反馈给学生所在学院。经核实确需更正成绩的，由任课教师、学院和教务处按流程完成成绩变更。

                教师调停课需提前在教务系统提交申请，说明调停课原因、补课时间和地点，经学院审核后报教务处备案。涉及全校公共课的调课安排应同步通知学生。

                实践教学、实习实训和课程设计应按照教学计划执行。因场地、天气或安全原因调整安排的，学院应提前发布通知并保留过程记录。
                """, "教务处", "TEACHER", admin);
        documentService.ingestText("办事指南-校园服务清单.md", """
                学籍证明、在读证明和成绩单可在自助打印终端办理。部分材料需要加盖学院或教务处公章，学生应根据用途提前确认接收单位要求。

                毕业生办理离校手续前，应完成图书归还、宿舍退宿、费用结清、校园卡注销或余额处理等事项。各部门办理状态可在离校系统中查看。

                图书馆借阅证与校园卡身份绑定。学生可凭校园卡借阅图书，逾期归还会产生相应限制。毕业离校前应结清图书借阅记录。

                图书遗失或损坏时，读者应及时到图书馆服务台登记处理。赔偿或补购方式以图书馆现行管理规定为准。

                网络账号初始密码应在首次登录后及时修改。如忘记密码，可通过统一身份认证平台自助找回，无法自助处理时联系信息化办公室。

                校园网故障报修应提供宿舍楼栋、房间号、联系方式和故障现象。信息化办公室会根据报修情况安排远程排查或现场处理。

                校园公共场地预约需登录场地预约系统。申请人应如实填写活动主题、人数、时间和安全责任人，审核通过后方可使用。

                大型活动需要提前完成安全预案、场地审批和设备申请。活动结束后，申请人应恢复场地原状并配合管理部门完成检查。
                """, "信息化办公室", "PUBLIC", admin);
    }

    private void seedQaAndFeedback() {
        if (qaRecordRepository.count() > 0) {
            return;
        }
        UserAccount student = userRepository.findByUsername("student").orElseThrow();
        UserAccount teacher = userRepository.findByUsername("teacher").orElseThrow();
        UserAccount admin = userRepository.findByUsername("admin").orElseThrow();

        QaRecord card = qa(student, "校园卡丢了怎么补办？",
                "校园卡遗失后，可以先在校园卡服务中心窗口或线上办事大厅办理挂失。补办时需要携带本人有效证件，工作日受理后通常当天可以领取新卡。原卡余额会在补卡完成后自动转入新卡。",
                "学生手册-综合服务规则.md", "学生工作处", "STUDENT", 0.914, 1186, LocalDateTime.now().minusHours(5));
        QaRecord exam = qa(student, "期末考试缓考需要什么材料？",
                "期末考试缓考申请应在考试前提交。因病申请缓考需提供医疗证明，因公或其他不可抗力原因申请缓考需提供相应证明材料。未经批准缺考的课程成绩按学校考试管理规定处理。",
                "教务管理-课程与考试指南.md", "教务处", "TEACHER", 0.887, 1320, LocalDateTime.now().minusHours(3));
        QaRecord network = qa(teacher, "校园网账号忘记密码怎么办？",
                "网络账号初始密码应在首次登录后及时修改。忘记密码时，可先通过统一身份认证平台自助找回；无法自助处理时，联系信息化办公室处理。",
                "办事指南-校园服务清单.md", "信息化办公室", "PUBLIC", 0.861, 1044, LocalDateTime.now().minusHours(2));

        qaRecordRepository.saveAll(List.of(card, exam, network));
        feedbackRepository.saveAll(List.of(
                feedback(card, student, "HELPFUL", "回答能直接说明挂失和余额转入流程。", LocalDateTime.now().minusHours(4).minusMinutes(40)),
                feedback(exam, student, "INCOMPLETE", "希望补充线上申请入口名称。", LocalDateTime.now().minusHours(2).minusMinutes(45)),
                feedback(network, admin, "SOURCE_WRONG", "来源命中文档正确，但最好展示信息化办公室联系方式。", LocalDateTime.now().minusHours(1).minusMinutes(20))));
    }

    private QaRecord qa(
            UserAccount user,
            String question,
            String answer,
            String fileName,
            String department,
            String visibility,
            double score,
            long latencyMs,
            LocalDateTime createdAt) {
        QaRecord record = new QaRecord();
        record.setUserId(user.getId());
        record.setUserName(user.getDisplayName());
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setLatencyMs(latencyMs);
        record.setCreatedAt(createdAt);
        record.setSourcesJson("""
                [{"id":"seed","chunkId":1,"documentId":1,"chunkNo":1,"pageNo":1,"content":"演示来源片段：系统从校园知识库中召回相关规定，并将来源文档、部门、可见范围和相似度一并保存。","fileName":"%s","department":"%s","visibility":"%s","score":%.3f}]
                """.formatted(fileName, department, visibility, score).trim());
        return record;
    }

    private Feedback feedback(QaRecord record, UserAccount user, String type, String comment, LocalDateTime createdAt) {
        Feedback feedback = new Feedback();
        feedback.setQaRecordId(record.getId());
        feedback.setUserId(user.getId());
        feedback.setType(type);
        feedback.setComment(comment);
        feedback.setCreatedAt(createdAt);
        return feedback;
    }

    private void seedLogs() {
        if (logRepository.count() > 0) {
            return;
        }
        UserAccount admin = userRepository.findByUsername("admin").orElseThrow();
        UserAccount teacher = userRepository.findByUsername("teacher").orElseThrow();
        UserAccount student = userRepository.findByUsername("student").orElseThrow();
        logRepository.saveAll(List.of(
                log(admin, "DOCUMENT_UPLOAD", "学生手册-综合服务规则.md", "上传并向量化学生手册演示资料", "127.0.0.1", true, LocalDateTime.now().minusHours(6)),
                log(admin, "DOCUMENT_REBUILD", "教务管理-课程与考试指南.md", "重新切片并同步向量元数据", "127.0.0.1", true, LocalDateTime.now().minusHours(4)),
                log(student, "RAG_ASK", "chat", "校园卡丢了怎么补办？", "127.0.0.1", true, LocalDateTime.now().minusHours(3).minusMinutes(40)),
                log(teacher, "RAG_ASK", "chat", "校园网账号忘记密码怎么办？", "127.0.0.1", true, LocalDateTime.now().minusHours(2).minusMinutes(10)),
                log(admin, "SYSTEM_CONFIG_UPDATE", "spark.enabled", "管理员调整大模型服务开关和检索阈值", "127.0.0.1", true, LocalDateTime.now().minusHours(1))));
    }

    private OperationLog log(
            UserAccount user,
            String action,
            String target,
            String detail,
            String ipAddress,
            boolean success,
            LocalDateTime createdAt) {
        OperationLog log = new OperationLog();
        log.setUserId(user.getId());
        log.setUserName(user.getDisplayName());
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        log.setSuccess(success);
        log.setCreatedAt(createdAt);
        return log;
    }
}
