param(
    [switch]$Apply
)

$ErrorActionPreference = 'Stop'
$sourceRoot = Join-Path $PSScriptRoot '..\backend\src\main\java\com\learningplatform'
$sourceRoot = (Resolve-Path $sourceRoot).Path
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

$moduleDescriptions = @{
    admin = '平台治理与管理员操作'
    ai = 'AI 任务、对话、分析与供应商调用'
    auth = '身份认证、JWT 与登录安全'
    classroom = '班级、成员、公告与班级资源范围'
    common = '统一协议、异常、配置与跨领域基础设施'
    content = '学习资料、分类、文件、审核与访问控制'
    exam = '试卷、考试、作答、阅卷、统计与错题'
    grading = '题型答案规范化与评分'
    learning = '学习进度、点赞、收藏与评论'
    offline = '线下教师申请、审核、检索与推荐'
    order = '商品、订单、支付模拟与用户权益'
    question = '题库、题目、选项与标准答案'
    user = '用户、角色、头像与公开个人中心'
    root = '应用启动与领域模块装配'
}

$layerDescriptions = @{
    web = 'HTTP 接口层'
    service = '业务服务层'
    mapper = 'MyBatis 持久化层'
    domain = '领域模型层'
    dto = '接口数据契约层'
    config = '配置装配层'
    security = '安全认证层'
    client = '外部服务适配层'
    storage = '对象存储层'
    text = '文本提取与规范化层'
    audit = '审计基础设施层'
    exception = '异常处理层'
    page = '分页基础模型层'
    model = '公共数据模型层'
}

$wordTranslations = @{
    admin = '管理'
    ai = 'AI'
    auth = '认证'
    authentication = '认证'
    authorization = '授权'
    user = '用户'
    role = '角色'
    profile = '资料'
    avatar = '头像'
    publisher = '发布者'
    content = '学习资料'
    category = '分类'
    file = '文件'
    upload = '上传'
    storage = '存储'
    access = '访问权'
    audit = '审核'
    learning = '学习'
    progress = '进度'
    comment = '评论'
    like = '点赞'
    favorite = '收藏'
    classroom = '班级'
    class = '班级'
    member = '成员'
    announcement = '公告'
    invite = '邀请码'
    scope = '范围'
    question = '题目'
    bank = '题库'
    option = '选项'
    answer = '答案'
    exam = '考试'
    paper = '试卷'
    candidate = '考生'
    attempt = '作答'
    result = '成绩'
    grading = '阅卷'
    grade = '评分'
    score = '分数'
    submission = '交卷'
    runtime = '运行态'
    wrong = '错题'
    review = '复习'
    order = '订单'
    product = '商品'
    payment = '支付'
    entitlement = '权益'
    quota = '额度'
    summary = '总结'
    analysis = '分析'
    generate = '生成'
    record = '记录'
    by = '按'
    create = '创建'
    update = '更新'
    save = '保存'
    replace = '替换'
    delete = '删除'
    remove = '移除'
    submit = '提交'
    publish = '发布'
    approve = '审核通过'
    reject = '驳回'
    cancel = '取消'
    complete = '完成'
    finish = '结束'
    start = '开始'
    resume = '恢复'
    grant = '发放'
    consume = '消费'
    find = '查询'
    search = '搜索'
    list = '列表'
    detail = '详情'
    latest = '最新'
    increment = '增加'
    decrement = '减少'
    conversation = '会话'
    message = '消息'
    task = '任务'
    usage = '用量'
    explanation = '讲解'
    template = '模板'
    prompt = '提示词'
    provider = '供应商'
    deep = 'Deep'
    seek = 'Seek'
    mock = '模拟'
    offline = '线下教学'
    teacher = '教师'
    teaching = '教学'
    recommendation = '推荐'
    preference = '学习需求'
    application = '申请'
    operation = '操作'
    log = '日志'
    health = '健康检查'
    trace = '链路追踪'
    rate = '频率'
    limit = '限制'
    sensitive = '敏感配置'
    configuration = '配置'
    config = '配置'
    properties = '配置属性'
    exception = '异常'
    error = '错误'
    response = '响应'
    request = '请求'
    input = '输入'
    output = '输出'
    article = '文章'
    mode = '模式'
    view = '浏览'
    rejection = '驳回'
    guard = '保护'
    max = '最大'
    per = '每个'
    window = '窗口'
    timeout = '超时'
    enabled = '启用状态'
    secret = '密钥'
    key = '键'
    value = '值'
    total = '总计'
    available = '可用'
    source = '来源'
    target = '目标'
    created = '创建'
    updated = '更新'
    effective = '生效'
    expires = '过期'
    query = '查询条件'
    page = '分页'
    id = 'ID'
    name = '名称'
    title = '标题'
    body = '正文'
    cover = '封面'
    count = '数量'
    reason = '原因'
    free = '免费状态'
    price = '价格'
    at = '时间'
    submitted = '提交'
    published = '发布'
    distribution = '发放'
    editable = '可编辑'
    status = '状态'
    type = '类型'
    code = '编码'
    lifecycle = '生命周期'
    persistence = '持久化'
    security = '安全'
    token = '令牌'
    jwt = 'JWT'
    principal = '认证主体'
    resolver = '解析器'
    filter = '过滤器'
    handler = '处理器'
    factory = '工厂'
    extractor = '提取器'
    validator = '校验器'
    scheduler = '调度器'
    controller = ''
    service = ''
    mapper = ''
    dto = ''
}

function Split-PascalCase([string]$value) {
    return [regex]::Matches(
        $value,
        '[A-Z]+(?=[A-Z][a-z]|[0-9]|$)|[A-Z]?[a-z]+|[0-9]+'
    ) | ForEach-Object { $_.Value }
}

function Convert-ToConcept([string]$value) {
    $parts = [System.Collections.Generic.List[string]]::new()
    foreach ($word in (Split-PascalCase $value)) {
        $key = $word.ToLowerInvariant()
        if ($wordTranslations.ContainsKey($key)) {
            $translated = $wordTranslations[$key]
            if (-not [string]::IsNullOrWhiteSpace($translated)) {
                $parts.Add($translated)
            }
        } else {
            $parts.Add($word)
        }
    }
    $result = ($parts -join '')
    $result = $result.Replace('学习学习资料', '学习资料')
    $result = $result.Replace('题目题目', '题目')
    $result = $result.Replace('AIClient', 'AI 客户端')
    $result = $result.Replace('DeepSeekAI 客户端', 'DeepSeek AI 客户端')
    if ([string]::IsNullOrWhiteSpace($result)) {
        return $value
    }
    return $result
}

function Get-RelativePath([System.IO.FileInfo]$file) {
    return $file.FullName.Substring($sourceRoot.Length + 1)
}

function Get-Module([System.IO.FileInfo]$file) {
    $relative = Get-RelativePath $file
    $segments = $relative -split '[\\/]'
    if ($segments.Count -eq 1) {
        return 'root'
    }
    return $segments[0]
}

function Get-Layer([System.IO.FileInfo]$file) {
    $relative = Get-RelativePath $file
    $segments = $relative -split '[\\/]'
    if ($segments.Count -le 2) {
        return 'module'
    }
    return $segments[1]
}

function Get-TypeKind([string]$text) {
    $match = [regex]::Match(
        $text,
        '(?m)^\s*(?:public\s+)?(?:abstract\s+|final\s+|sealed\s+|non-sealed\s+)?(?<kind>@interface|class|interface|record|enum)\s+\w+'
    )
    if ($match.Success) {
        return $match.Groups['kind'].Value
    }
    return 'class'
}

function Get-FileDescription([System.IO.FileInfo]$file, [string]$text) {
    if ($file.Name -eq 'package-info.java') {
        return '说明本模块总体职责、分层边界以及全部源码文件的用途。'
    }

    $name = $file.BaseName
    $concept = Convert-ToConcept $name
    $kind = Get-TypeKind $text
    switch -Regex ($name) {
        'Controller$' { return "提供${concept}相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。" }
        'Service$' { return "实现${concept}业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。" }
        'Mapper$' { return "定义${concept}的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。" }
        'Configuration$|Config$' { return "装配${concept}运行配置和依赖组件，并对关键配置项执行启动期校验。" }
        'Properties$' { return "承载${concept}配置属性，供配置装配和业务组件以类型安全方式读取。" }
        'Filter$' { return "在 Servlet 过滤链中处理${concept}，并在请求进入 Controller 前建立安全或上下文约束。" }
        'Resolver$' { return "集中解析${concept}，避免各调用方重复实现协议或身份转换逻辑。" }
        'Handler$' { return "统一处理${concept}场景并转换为平台约定的结果。" }
        'Client$' { return "定义或实现${concept}外部调用适配，隔离供应商协议与业务服务。" }
        'Factory$' { return "集中创建${concept}，保证不同调用场景使用一致规则。" }
        'Extractor$' { return "从输入中提取并规范化${concept}，为后续业务或 AI 调用提供安全文本。" }
        'Validator$' { return "校验${concept}的格式和业务不变量，失败时返回明确错误。" }
        'Guard$' { return "保护${concept}调用的频率、并发和超时边界，并把底层失败转换为安全错误。" }
        'Scheduler$' { return "按配置周期调度${concept}，并把实际业务处理委托给服务层。" }
        'Exception$' { return "表示${concept}失败，携带可由统一异常处理器转换的错误语义。" }
        'Request$' { return "定义${concept}接口的请求字段和 Bean Validation 约束。" }
        'Response$' { return "定义${concept}接口的只读返回契约，避免直接暴露数据库实体。" }
        'Query$' { return "定义${concept}列表或检索接口的查询条件、分页参数和默认值。" }
        'Status$|Type$|Code$|Role$|Mode$|Format$' {
            if ($kind -eq 'enum') {
                return "枚举${concept}允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。"
            }
        }
    }

    switch ($kind) {
        'interface' { return "定义${concept}能力接口，使调用方依赖稳定抽象而非具体实现。" }
        'record' { return "以不可变记录表示${concept}数据，并作为模块内部或接口层的数据契约。" }
        'enum' { return "枚举${concept}允许的有限取值，供持久化、校验和状态分支共同使用。" }
        '@interface' { return "定义${concept}注解，用于以声明式方式标记相关行为。" }
        default { return "表示${concept}领域对象或组件，封装该概念相关的数据和行为。" }
    }
}

function Get-TypeBoundary([System.IO.FileInfo]$file) {
    $module = Get-Module $file
    $layer = Get-Layer $file
    switch ($layer) {
        'web' { return '只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。' }
        'service' { return '业务状态变化在此集中完成；跨表写入需保持事务一致性。' }
        'mapper' { return '只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。' }
        'domain' { return '保存领域状态，不依赖 Web 层，也不负责发起外部调用。' }
        'dto' { return '字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。' }
        'config' { return '只负责组件装配和配置校验，不承载具体业务流程。' }
        'security' { return '建立请求身份与安全上下文，资源级权限仍由领域服务校验。' }
        'client' { return '负责协议转换、超时和安全日志，不直接扣减业务权益。' }
        'storage' { return '对象存储保持私有，外部访问只能使用受控的短期签名地址。' }
        default { return "遵守 $($moduleDescriptions[$module]) 模块的职责边界。" }
    }
}

function Has-Documentation(
    [System.Collections.Generic.List[string]]$lines,
    [int]$index
) {
    $minimum = [Math]::Max(0, $index - 20)
    for ($cursor = $index - 1; $cursor -ge $minimum; $cursor--) {
        $value = $lines[$cursor].Trim()
        if ($value.Length -eq 0) {
            continue
        }
        if ($value.StartsWith('@')) {
            continue
        }
        if ($value.EndsWith('*/') -or $value.StartsWith('//')) {
            return $true
        }
        return $false
    }
    return $false
}

function Is-GeneratedMemberComment([string]$value) {
    if (-not ($value.Trim().StartsWith('/**') -and $value.Trim().EndsWith('*/'))) {
        return $false
    }
    return $value.Contains('供该类型的业务逻辑读取或更新') `
        -or $value.Trim() -match '^/\*\* 返回.*。 \*/$' `
        -or $value.Contains('调用方仍需遵守所属领域的校验规则') `
        -or $value.Contains('注入并保存该组件运行所需依赖') `
        -or $value.Contains('完成参数接收、当前用户解析并返回统一 API 响应') `
        -or $value.Contains('只返回当前调用方有权查看的结果') `
        -or $value.Contains('不满足时抛出明确业务异常') `
        -or $value.Contains('维护唯一性、初始状态和必要关联') `
        -or $value.Contains('通过返回值或版本条件识别并发状态变化') `
        -or $value.Contains('同时维护关联数据和权限不变量') `
        -or $value.Contains('仅允许从合法前置状态进入目标状态') `
        -or $value.Contains('失败不会留下不一致的持久化结果') `
        -or $value.Contains('不引入额外持久化副作用') `
        -or $value.Contains('写操作返回受影响行数供服务层判断状态') `
        -or $value.Contains('对应的新记录，并返回受影响行数或回填生成主键') `
        -or $value.Contains('具体输入输出由方法签名和所属类型共同约束') `
        -or $value.Contains('执行对应领域规则') `
        -or $value.Contains('负责受控 JSON 序列化和反序列化') `
        -or $value.Contains('提供可替换时间源') `
        -or $value.Contains('记录关键状态变化') `
        -or $value.Contains('定义该组件使用的固定规则或默认值')
}

function Find-NearestMapping(
    [System.Collections.Generic.List[string]]$lines,
    [int]$index
) {
    $minimum = [Math]::Max(0, $index - 15)
    for ($cursor = $index - 1; $cursor -ge $minimum; $cursor--) {
        $value = $lines[$cursor].Trim()
        $mapping = [regex]::Match(
            $value,
            '^@(?<verb>Get|Post|Put|Patch|Delete)Mapping(?:\(\"(?<path>[^\"]*)\"\))?'
        )
        if ($mapping.Success) {
            $path = $mapping.Groups['path'].Value
            if ([string]::IsNullOrEmpty($path)) {
                $path = '当前资源'
            }
            return "$($mapping.Groups['verb'].Value.ToUpperInvariant()) $path"
        }
    }
    return $null
}

function Get-FieldDescription([string]$name, [string]$typeName) {
    switch ($name) {
        'log' { return '记录关键状态变化、外部调用阶段和可关联 traceId 的安全日志。' }
        'logger' { return '记录关键状态变化和异常上下文，不输出密码、密钥或敏感正文。' }
        'COLUMNS' { return '复用学习资料查询列，保证不同查询返回一致字段集合。' }
        'SYSTEM_PROMPT' { return '约束 AI 的任务、输出格式和安全边界，防止执行用户数据中的指令。' }
    }
    if ($name -cmatch '^[A-Z][A-Z0-9_]+$') {
        return "定义 $name 常量，统一该组件使用的固定规则或默认值。"
    }
    if ($name.EndsWith('Mapper')) {
        return "访问$(Convert-ToConcept $name)持久化数据。"
    }
    if ($name.EndsWith('Service')) {
        return "委托$(Convert-ToConcept $name)执行对应领域规则。"
    }
    if ($name.EndsWith('Client')) {
        return "通过$(Convert-ToConcept $name)调用隔离后的外部能力。"
    }
    if ($name -eq 'objectMapper') {
        return '负责受控 JSON 序列化和反序列化。'
    }
    if ($name -eq 'clock') {
        return '提供可替换时间源，便于测试时间相关规则。'
    }
    return "保存$(Convert-ToConcept $name)，供该类型的业务逻辑读取或更新。"
}

function Get-MethodDescription(
    [string]$name,
    [string]$fileBaseName,
    [string]$layer,
    [string]$mapping
) {
    if ($name -eq $fileBaseName) {
        return '注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。'
    }
    if ($name -eq 'toString') {
        return '返回适合日志记录的文本表示；敏感 DTO 必须对密码、令牌或证件信息脱敏。'
    }
    if ($fileBaseName -eq 'AiClient' -and $name -eq 'provider') {
        return '返回当前 AI 供应商标识，用于任务记录、用量审计和运行日志。'
    }
    if ($fileBaseName -eq 'AiClient' -and $name -eq 'model') {
        return '返回当前 AI 模型标识，用于任务快照和问题追踪。'
    }
    if ($fileBaseName -eq 'AiClient' -and $name -eq 'complete') {
        return '发送一次对话补全请求并返回供应商无关的规范化结果；失败时抛出 AI 客户端异常。'
    }
    if (-not [string]::IsNullOrWhiteSpace($mapping)) {
        return "处理 $mapping 请求，完成参数接收、当前用户解析并返回统一 API 响应。"
    }
    if ($layer -eq 'mapper') {
        if ($name -match '^(find|search|list|count|sum|exists|has)(.*)$') {
            return "执行 $name 数据库查询，返回领域对象、聚合值或是否存在的判断结果。"
        }
        if ($name -match '^(insert|create)(.*)$') {
            return '插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。'
        }
        if ($name -match '^(update|replace|mark|increment|decrement|consume|approve|reject|submit|publish|cancel|complete|delete|remove|softDelete)(.*)$') {
            return "执行 $name 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。"
        }
    }
    if ($name -match '^get([A-Z].*)$') {
        return "返回$(Convert-ToConcept $Matches[1])。"
    }
    if ($name -match '^set([A-Z].*)$') {
        return "更新$(Convert-ToConcept $Matches[1])；调用方仍需遵守所属领域的校验规则。"
    }
    if ($name -match '^(is|has|can)([A-Z].*)$') {
        return "判断是否满足$(Convert-ToConcept $Matches[2])条件，不修改持久化状态。"
    }
    if ($name -match '^(find|search|query|list|detail|overview|latest|load|read)(.*)$') {
        $target = Convert-ToConcept $Matches[2]
        if ([string]::IsNullOrWhiteSpace($target)) {
            $target = '目标'
        }
        if ($target.StartsWith('按')) {
            return "${target}查询数据；只返回当前调用方有权查看的结果。"
        }
        return "查询${target}相关数据；只返回当前调用方有权查看的结果。"
    }
    if ($name -match '^(require|ensure|assert|validate|check)(.*)$') {
        return "校验$(Convert-ToConcept $Matches[2])及相关业务前置条件，不满足时抛出明确业务异常。"
    }
    if ($name -match '^(create|insert|register|join|start|initialize|grant|issue)(.*)$') {
        return "创建或初始化$(Convert-ToConcept $Matches[2])，并维护唯一性、初始状态和必要关联。"
    }
    if ($name -match '^(update|save|replace|change|refresh|regenerate|restore|resume)(.*)$') {
        return "更新$(Convert-ToConcept $Matches[2])，通过返回值或版本条件识别并发状态变化。"
    }
    if ($name -match '^(delete|remove|clear|leave|revoke)(.*)$') {
        return "删除、移除或清理$(Convert-ToConcept $Matches[2])，同时维护关联数据和权限不变量。"
    }
    if ($name -match '^(submit|publish|approve|reject|cancel|complete|finish|offline|activate|suspend|fail|close)(.*)$') {
        return "执行$(Convert-ToConcept $Matches[1])状态流转，仅允许从合法前置状态进入目标状态。"
    }
    if ($name -match '^(generate|analyze|explain|recommend|score|grade|calculate|consume)(.*)$') {
        return "执行$(Convert-ToConcept $name)核心计算或业务处理，并保证失败不会留下不一致的持久化结果。"
    }
    if ($name -match '^(to|from|of|map|convert|normalize|parse|format)(.*)$') {
        return "转换或规范化$(Convert-ToConcept $Matches[2])数据，不引入额外持久化副作用。"
    }
    if ($layer -eq 'mapper') {
        return "执行 $name 对应的数据库操作；写操作返回受影响行数供服务层判断状态。"
    }
    if ($layer -eq 'service') {
        return "执行 $name 对应的领域用例，并在服务层维护权限、事务和状态约束。"
    }
    return "执行 $name 对应职责；具体输入输出由方法签名和所属类型共同约束。"
}

function Get-MethodMatch([string]$trimmed, [bool]$topLevelInterface) {
    if ($trimmed.StartsWith('//') -or $trimmed.StartsWith('*') -or $trimmed.StartsWith('@') -or $trimmed.Contains('->')) {
        return $null
    }
    if ($trimmed -match '^(if|for|while|switch|catch|return|throw|new|try|else|do)\b') {
        return $null
    }
    $withVisibility = [regex]::Match(
        $trimmed,
        '^(?:public|protected|private)\s+(?:(?:static|final|synchronized|abstract|default|native)\s+)*(?:<[^>]+>\s+)?(?:(?<return>[A-Za-z_$][\w$.\<\>, ?\[\]]*)\s+)?(?<name>[A-Za-z_$][\w$]*)\s*\('
    )
    if ($withVisibility.Success) {
        return $withVisibility
    }
    if ($topLevelInterface) {
        $interfaceMethod = [regex]::Match(
            $trimmed,
            '^(?:(?:default|static|abstract)\s+)*(?:<[^>]+>\s+)?(?<return>[A-Za-z_$][\w$.\<\>, ?\[\]]*)\s+(?<name>[A-Za-z_$][\w$]*)\s*\('
        )
        if ($interfaceMethod.Success) {
            return $interfaceMethod
        }
    }
    return $null
}

function Add-SourceComments(
    [System.IO.FileInfo]$file,
    [string]$text
) {
    if ($file.Name -eq 'package-info.java') {
        return $text
    }
    $newline = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
    $lines = [System.Collections.Generic.List[string]]::new()
    foreach ($line in ($text -split '\r?\n')) {
        $lines.Add($line)
    }

    $description = Get-FileDescription $file $text
    $module = Get-Module $file
    $layer = Get-Layer $file
    $layerDescription = $layerDescriptions[$layer]
    if ([string]::IsNullOrWhiteSpace($layerDescription)) {
        $layerDescription = '模块根目录'
    }

    # 删除旧版本脚本可能误插入 Java 文本块（主要是 Mapper SQL）中的成员注释。
    $cleanedLines = [System.Collections.Generic.List[string]]::new()
    $insideTextBlock = $false
    foreach ($line in $lines) {
        $delimiterCount = [regex]::Matches($line, '"""').Count
        if ($insideTextBlock -and $line.Trim().StartsWith('/**') -and $line.Trim().EndsWith('*/')) {
            continue
        }
        $cleanedLines.Add($line)
        if (($delimiterCount % 2) -eq 1) {
            $insideTextBlock = -not $insideTextBlock
        }
    }
    $lines = $cleanedLines

    if (-not $text.StartsWith('/* 文件职责：')) {
        $header = [string[]]@(
            "/* 文件职责：$description",
            " * 所属模块：$($moduleDescriptions[$module])；所在分层：$layerDescription。",
            " * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。",
            ' */'
        )
        $lines.InsertRange(0, $header)
    } else {
        # 已生成的文件头随词典和职责规则更新，人工编写的类型 Javadoc 保持不动。
        $lines[0] = "/* 文件职责：$description"
        $lines[1] = " * 所属模块：$($moduleDescriptions[$module])；所在分层：$layerDescription。"
    }

    $typePattern = '^\s*(?:public\s+)?(?:abstract\s+|final\s+|sealed\s+|non-sealed\s+)?(?<kind>@interface|class|interface|record|enum)\s+(?<name>\w+)'
    $topLevelInterface = (Get-TypeKind $text) -eq 'interface'
    $typeDocumented = $false
    $insideTextBlock = $false
    for ($index = 0; $index -lt $lines.Count; $index++) {
        $line = $lines[$index]
        $trimmed = $line.Trim()
        $delimiterCount = [regex]::Matches($line, '"""').Count
        if ($insideTextBlock) {
            if (($delimiterCount % 2) -eq 1) {
                $insideTextBlock = $false
            }
            continue
        }
        if (($delimiterCount % 2) -eq 1) {
            $textBlockFieldMatch = [regex]::Match(
                $trimmed,
                '^(?:(?:public|protected|private)\s+)?(?:(?:static|final|volatile|transient)\s+)*(?<type>[A-Za-z_$][\w$.\<\>, ?\[\]]*)\s+(?<name>[A-Za-z_$][\w$]*)\s*=.*"""'
            )
            if ($textBlockFieldMatch.Success) {
                $indent = $line.Substring(0, $line.Length - $line.TrimStart().Length)
                $fieldDescription = Get-FieldDescription `
                    $textBlockFieldMatch.Groups['name'].Value `
                    $textBlockFieldMatch.Groups['type'].Value
                if ($index -gt 0 -and (Is-GeneratedMemberComment $lines[$index - 1])) {
                    $lines[$index - 1] = "$indent/** $fieldDescription */"
                } elseif (-not (Has-Documentation $lines $index)) {
                    $lines.Insert($index, "$indent/** $fieldDescription */")
                    $index++
                }
            }
            $insideTextBlock = $true
            continue
        }

        $typeMatch = [regex]::Match($line, $typePattern)
        if ($typeMatch.Success -and -not $typeDocumented) {
            if (-not (Has-Documentation $lines $index)) {
                $typeDoc = [string[]]@(
                    '/**',
                    " * $description",
                    ' *',
                    " * <p>职责边界：$(Get-TypeBoundary $file)</p>",
                    ' */'
                )
                $lines.InsertRange($index, $typeDoc)
                $index += $typeDoc.Count
            } else {
                $docStart = -1
                for ($cursor = $index - 1; $cursor -ge [Math]::Max(0, $index - 12); $cursor--) {
                    if ($lines[$cursor].Trim() -eq '/**') {
                        $docStart = $cursor
                        break
                    }
                }
                if ($docStart -ge 0) {
                    $docText = [string]::Join("`n", $lines.GetRange($docStart, $index - $docStart))
                    if ($docText.Contains('职责边界：')) {
                        $lines[$docStart + 1] = " * $description"
                        for ($cursor = $docStart + 2; $cursor -lt $index; $cursor++) {
                            if ($lines[$cursor].Contains('职责边界：')) {
                                $lines[$cursor] = " * <p>职责边界：$(Get-TypeBoundary $file)</p>"
                                break
                            }
                        }
                    }
                }
            }
            $typeDocumented = $true
            continue
        }

        if ($trimmed -notmatch '\(') {
            $fieldMatch = [regex]::Match(
                $trimmed,
                '^(?:public|protected|private)\s+(?:(?:static|final|volatile|transient)\s+)*(?<type>[A-Za-z_$][\w$.\<\>, ?\[\]]*)\s+(?<name>[A-Za-z_$][\w$]*)\s*(?:=.*|;)$'
            )
            if (-not $fieldMatch.Success -and $topLevelInterface) {
                $fieldMatch = [regex]::Match(
                    $trimmed,
                    '^(?<type>[A-Za-z_$][\w$.\<\>, ?\[\]]*)\s+(?<name>[A-Z][A-Z0-9_]*)\s*=.*$'
                )
            }
            if ($fieldMatch.Success) {
                $indent = $line.Substring(0, $line.Length - $line.TrimStart().Length)
                $fieldDescription = Get-FieldDescription `
                    $fieldMatch.Groups['name'].Value `
                    $fieldMatch.Groups['type'].Value
                if ($index -gt 0 -and (Is-GeneratedMemberComment $lines[$index - 1])) {
                    $lines[$index - 1] = "$indent/** $fieldDescription */"
                } elseif (-not (Has-Documentation $lines $index)) {
                    $lines.Insert($index, "$indent/** $fieldDescription */")
                    $index++
                }
                continue
            }
        }

        $methodMatch = Get-MethodMatch $trimmed $topLevelInterface
        $hasGeneratedMethodComment = $index -gt 0 -and (Is-GeneratedMemberComment $lines[$index - 1])
        if ($null -ne $methodMatch -and $methodMatch.Success -and ($hasGeneratedMethodComment -or -not (Has-Documentation $lines $index))) {
            $indent = $line.Substring(0, $line.Length - $line.TrimStart().Length)
            $methodName = $methodMatch.Groups['name'].Value
            $mapping = Find-NearestMapping $lines $index
            $methodDescription = Get-MethodDescription `
                $methodName `
                $file.BaseName `
                $layer `
                $mapping
            if ($hasGeneratedMethodComment) {
                $lines[$index - 1] = "$indent/** $methodDescription */"
            } else {
                $lines.Insert($index, "$indent/** $methodDescription */")
                $index++
            }
        }
    }
    return [string]::Join($newline, $lines)
}

function Update-PackageIndex(
    [System.IO.FileInfo]$packageInfo,
    [System.IO.FileInfo[]]$moduleFiles,
    [string]$text
) {
    $newline = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
    $startMarker = ' * <!-- FILE_INDEX_START -->'
    $endMarker = ' * <!-- FILE_INDEX_END -->'
    $start = $text.IndexOf($startMarker)
    if ($start -ge 0) {
        $end = $text.IndexOf($endMarker, $start)
        if ($end -lt 0) {
            throw "文件索引缺少结束标记：$($packageInfo.FullName)"
        }
        $end += $endMarker.Length
        $text = $text.Remove($start, $end - $start)
        $text = [regex]::Replace(
            $text,
            '(\r?\n)+(?=\s*\*/)',
            $newline
        )
    }

    $moduleDirectory = $packageInfo.Directory.FullName
    $indexLines = [System.Collections.Generic.List[string]]::new()
    $indexLines.Add($startMarker)
    $indexLines.Add(' * <h2>文件职责索引</h2>')
    $indexLines.Add(' * <ul>')
    foreach ($file in ($moduleFiles | Sort-Object FullName)) {
        $relative = $file.FullName.Substring($moduleDirectory.Length + 1).Replace('\', '/')
        $fileText = [System.IO.File]::ReadAllText($file.FullName)
        $description = Get-FileDescription $file $fileText
        $indexLines.Add(" *   <li>{@code $relative}：$description</li>")
    }
    $indexLines.Add(' * </ul>')
    $indexLines.Add($endMarker)
    $index = [string]::Join($newline, $indexLines)
    $closing = $text.LastIndexOf('*/')
    if ($closing -lt 0) {
        throw "package-info.java 缺少 Javadoc：$($packageInfo.FullName)"
    }
    return $text.Insert($closing, "$index$newline")
}

$javaFiles = @(
    Get-ChildItem $sourceRoot -Recurse -Filter '*.java' |
        Sort-Object FullName
)
$changes = [System.Collections.Generic.List[object]]::new()

foreach ($file in $javaFiles | Where-Object Name -ne 'package-info.java') {
    $original = [System.IO.File]::ReadAllText($file.FullName)
    $updated = Add-SourceComments $file $original
    if ($updated -ne $original) {
        $changes.Add([pscustomobject]@{
            File = $file
            Content = $updated
            Kind = 'source-comments'
        })
    }
}

$packageFiles = @($javaFiles | Where-Object Name -eq 'package-info.java')
foreach ($packageInfo in $packageFiles) {
    $moduleFiles = @(
        $javaFiles | Where-Object {
            $_.FullName.StartsWith($packageInfo.Directory.FullName + [IO.Path]::DirectorySeparatorChar)
        }
    )
    $original = [System.IO.File]::ReadAllText($packageInfo.FullName)
    $updated = Update-PackageIndex $packageInfo $moduleFiles $original
    if ($updated -ne $original) {
        $changes.Add([pscustomobject]@{
            File = $packageInfo
            Content = $updated
            Kind = 'package-index'
        })
    }
}

$sourceChangeCount = @($changes | Where-Object Kind -eq 'source-comments').Count
$packageChangeCount = @($changes | Where-Object Kind -eq 'package-index').Count
Write-Output "Java files: $($javaFiles.Count)"
Write-Output "Source comment updates: $sourceChangeCount"
Write-Output "Package index updates: $packageChangeCount"

if (-not $Apply) {
    Write-Output 'Dry run only. Re-run with -Apply to write changes.'
    exit 0
}

foreach ($change in $changes) {
    [System.IO.File]::WriteAllText(
        $change.File.FullName,
        $change.Content,
        $utf8NoBom
    )
}
Write-Output "Applied $($changes.Count) file updates."
