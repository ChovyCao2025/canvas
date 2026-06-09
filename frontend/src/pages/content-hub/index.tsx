import { useEffect, useMemo, useState, type ChangeEvent } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  CheckCircleOutlined,
  CloudUploadOutlined,
  EyeOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  PictureOutlined,
  ReloadOutlined,
  RollbackOutlined,
  SafetyCertificateOutlined,
  SaveOutlined,
  SearchOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons'
import { marketingContentApi } from '../../services/marketingContentApi'
import {
  assetTypeLabel,
  channelLabel,
  contentStatusView,
  durationLabel,
  extractTemplateVariables,
  formatMissingVariables,
  groupReleaseBlockers,
  localTemplatePreview,
  parseAssetRefs,
  releaseKeyFor,
  releaseSourceLabel,
  releaseStatusView,
  assetKeyFromFileName,
  assetTypeFromMime,
  uploadIntentStorageUrl,
  type ContentAuditEvent,
  type ContentEntry,
  type ContentEntryDraft,
  type ContentRelease,
  type ContentTemplate,
  type ContentTemplateDraft,
  type MarketingAsset,
  type MarketingAssetDraft,
  type MarketingAssetFolder,
  type MarketingAssetFolderDraft,
  type MarketingAssetUploadIntent,
  type ReleaseValidationResult,
  type ResolvedContentRelease,
  type TemplatePreviewResult,
} from './contentHubPresentation'

const { Title, Text, Paragraph } = Typography

const ASSET_TYPE_OPTIONS = ['IMAGE', 'VIDEO', 'FILE', 'AUDIO'].map(value => ({ value, label: assetTypeLabel(value) }))
const ASSET_STATUS_OPTIONS = ['DRAFT', 'READY', 'REJECTED', 'ARCHIVED'].map(value => ({
  value,
  label: contentStatusView(value).text,
}))
const TRANSCODE_STATUS_OPTIONS = ['PENDING', 'READY', 'FAILED', 'EXTERNAL'].map(value => ({ value, label: value }))
const CHANNEL_OPTIONS = ['EMAIL', 'SMS', 'PUSH', 'WECHAT', 'IN_APP', 'WEB', 'VIDEO'].map(value => ({
  value,
  label: channelLabel(value),
}))
const TEMPLATE_STATUS_OPTIONS = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ARCHIVED'].map(value => ({
  value,
  label: contentStatusView(value).text,
}))
const ENTRY_STATUS_OPTIONS = ['DRAFT', 'PUBLISHED', 'ARCHIVED'].map(value => ({
  value,
  label: contentStatusView(value).text,
}))
const CONTENT_TYPE_OPTIONS = ['ARTICLE', 'LANDING_PAGE', 'FAQ', 'LEGAL', 'SNIPPET'].map(value => ({ value, label: value }))

interface AssetSearchValues {
  keyword?: string
  assetType?: string
  status?: string
}

interface TemplateSearchValues {
  keyword?: string
  channel?: string
  status?: string
}

interface EntrySearchValues {
  keyword?: string
  contentType?: string
  status?: string
}

interface AssetEditorValues extends Omit<MarketingAssetDraft, 'tags' | 'metadata'> {
  tagsText?: string
  metadataJson?: string
}

interface TemplateEditorValues extends ContentTemplateDraft {
  previewContextJson?: string
}

type EntryEditorValues = ContentEntryDraft

interface ReleaseFormValues {
  sourceType: 'TEMPLATE' | 'ENTRY'
  sourceKey?: string
  contextJson?: string
  rollbackReason?: string
}

/** 将可选表单文本规整为 undefined，避免空字符串进入内容 API。 */
function trimText(value?: string | null) {
  const text = value?.trim()
  return text || undefined
}

/** 解析逗号分隔的资产标签，并按首次出现顺序去重。 */
function parseTags(value?: string) {
  return (value ?? '')
    .split(',')
    .map(item => item.trim())
    .filter((item, index, all) => item.length > 0 && all.indexOf(item) === index)
}

/** 解析编辑器 JSON 文本，确保内容正文、SEO 和元数据均为对象。 */
function parseJsonObject(value?: string): Record<string, unknown> {
  const text = value?.trim()
  if (!text) return {}
  const parsed = JSON.parse(text) as unknown
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('JSON 必须是对象')
  }
  return parsed as Record<string, unknown>
}

/** 校验 JSON 文本并在为空时填入默认结构，供模板/内容草稿保存使用。 */
function normalizeJsonText(value: string | undefined, fallback: string) {
  const text = value?.trim()
  if (!text) return fallback
  JSON.parse(text)
  return text
}

/** 按内容状态生成统一状态标签。 */
function statusTag(status: string) {
  const view = contentStatusView(status)
  return <Tag color={view.color}>{view.text}</Tag>
}

/** 内容中心页面，串联 DAM 资产、CMS 条目、模板预览和生产发布闭环。 */
export default function ContentHubPage() {
  const [assetSearchForm] = Form.useForm<AssetSearchValues>()
  const [templateSearchForm] = Form.useForm<TemplateSearchValues>()
  const [entrySearchForm] = Form.useForm<EntrySearchValues>()
  const [folderForm] = Form.useForm<MarketingAssetFolderDraft>()
  const [assetForm] = Form.useForm<AssetEditorValues>()
  const [templateForm] = Form.useForm<TemplateEditorValues>()
  const [entryForm] = Form.useForm<EntryEditorValues>()
  const [releaseForm] = Form.useForm<ReleaseFormValues>()

  const [assets, setAssets] = useState<MarketingAsset[]>([])
  const [assetFolders, setAssetFolders] = useState<MarketingAssetFolder[]>([])
  const [templates, setTemplates] = useState<ContentTemplate[]>([])
  const [entries, setEntries] = useState<ContentEntry[]>([])
  const [releases, setReleases] = useState<ContentRelease[]>([])
  const [auditEvents, setAuditEvents] = useState<ContentAuditEvent[]>([])
  const [selectedAsset, setSelectedAsset] = useState<MarketingAsset | null>(null)
  const [selectedTemplate, setSelectedTemplate] = useState<ContentTemplate | null>(null)
  const [selectedEntry, setSelectedEntry] = useState<ContentEntry | null>(null)
  const [preview, setPreview] = useState<TemplatePreviewResult | null>(null)
  const [releaseValidation, setReleaseValidation] = useState<ReleaseValidationResult | null>(null)
  const [resolvedRelease, setResolvedRelease] = useState<ResolvedContentRelease | null>(null)
  const [selectedUploadFile, setSelectedUploadFile] = useState<File | null>(null)
  const [uploadIntent, setUploadIntent] = useState<MarketingAssetUploadIntent | null>(null)
  const [loading, setLoading] = useState(false)
  const [savingFolder, setSavingFolder] = useState(false)
  const [savingAsset, setSavingAsset] = useState(false)
  const [uploadingAsset, setUploadingAsset] = useState(false)
  const [savingTemplate, setSavingTemplate] = useState(false)
  const [savingEntry, setSavingEntry] = useState(false)
  const [previewing, setPreviewing] = useState(false)
  const [releaseAction, setReleaseAction] = useState<'validate' | 'publish' | 'resolve' | 'rollback' | null>(null)
  const [error, setError] = useState<string | null>(null)

  const watchedSubject = Form.useWatch('subject', templateForm) ?? ''
  const watchedBody = Form.useWatch('body', templateForm) ?? ''
  const watchedReleaseSourceType = Form.useWatch('sourceType', releaseForm) ?? 'TEMPLATE'
  const watchedReleaseSourceKey = Form.useWatch('sourceKey', releaseForm)
  // 从当前模板草稿实时派生变量列表，辅助运营补齐预览上下文。
  const draftVariables = useMemo(() => extractTemplateVariables(watchedSubject, watchedBody), [watchedSubject, watchedBody])
  // 将资产目录转换为 Select 展示模型。
  const folderOptions = useMemo(() => assetFolders.map(folder => ({
    value: folder.id,
    label: folder.name,
  })), [assetFolders])
  // 根据发布来源类型切换模板或内容条目候选。
  const releaseSourceOptions = useMemo(() => {
    if (watchedReleaseSourceType === 'ENTRY') {
      return entries.map(entry => ({
        value: entry.entryKey,
        label: `${entry.title} (${entry.entryKey})`,
      }))
    }
    return templates.map(template => ({
      value: template.templateKey,
      label: `${template.displayName} (${template.templateKey})`,
    }))
  }, [entries, templates, watchedReleaseSourceType])
  // 找到当前来源最新的 ACTIVE 发布快照，作为解析和回滚默认目标。
  const activeRelease = useMemo(() => releases
    .filter(release => release.sourceType === watchedReleaseSourceType
      && release.sourceKey === watchedReleaseSourceKey
      && release.status === 'ACTIVE')
    .sort((left, right) => (right.sourceVersion ?? 0) - (left.sourceVersion ?? 0))[0] ?? null,
  [releases, watchedReleaseSourceKey, watchedReleaseSourceType])
  const currentReleaseKey = watchedReleaseSourceKey
    ? activeRelease?.releaseKey ?? releaseKeyFor(watchedReleaseSourceType, watchedReleaseSourceKey)
    : undefined
  // 审计事件按当前 releaseKey 过滤，只展示最近 5 条。
  const currentAuditEvents = useMemo(() => auditEvents
    .filter(event => !currentReleaseKey || event.targetKey === currentReleaseKey)
    .slice(0, 5),
  [auditEvents, currentReleaseKey])
  // 将发布阻断项按 scope 分组，便于门禁面板按对象类型展示。
  const releaseBlockerGroups = useMemo(
    () => groupReleaseBlockers(releaseValidation?.blockers ?? []),
    [releaseValidation],
  )
  const folderName = (folderId?: number | null) => assetFolders.find(folder => folder.id === folderId)?.name ?? '-'

  /** 选择资产并把后端展示模型回填到资产编辑表单。 */
  const selectAsset = (asset: MarketingAsset) => {
    setSelectedAsset(asset)
    assetForm.setFieldsValue({
      assetKey: asset.assetKey,
      name: asset.name,
      assetType: asset.assetType,
      mimeType: asset.mimeType,
      storageUrl: asset.storageUrl,
      folderId: asset.folderId ?? undefined,
      status: asset.status,
      tagsText: asset.tags.join(', '),
      durationMs: asset.durationMs ?? undefined,
      transcodeStatus: asset.transcodeStatus ?? undefined,
      posterUrl: asset.posterUrl ?? undefined,
      metadataJson: '{}',
    })
  }

  /** 选择模板并同步发布来源、预览状态和模板编辑表单。 */
  const selectTemplate = (template: ContentTemplate) => {
    setSelectedTemplate(template)
    setPreview(null)
    setReleaseValidation(null)
    setResolvedRelease(null)
    releaseForm.setFieldsValue({ sourceType: 'TEMPLATE', sourceKey: template.templateKey })
    templateForm.setFieldsValue({
      templateKey: template.templateKey,
      displayName: template.displayName,
      channel: template.channel,
      subject: template.subject ?? '',
      body: template.body,
      designJson: template.designJson || '{}',
      assetRefsJson: template.assetRefsJson || '[]',
      status: template.status,
      previewContextJson: '{}',
    })
  }

  /** 选择 CMS 内容条目并同步发布来源和编辑表单。 */
  const selectEntry = (entry: ContentEntry) => {
    setSelectedEntry(entry)
    setReleaseValidation(null)
    setResolvedRelease(null)
    releaseForm.setFieldsValue({ sourceType: 'ENTRY', sourceKey: entry.entryKey })
    entryForm.setFieldsValue({
      entryKey: entry.entryKey,
      contentType: entry.contentType,
      title: entry.title,
      slug: entry.slug ?? undefined,
      locale: entry.locale ?? undefined,
      summary: undefined,
      bodyJson: entry.bodyJson || '{}',
      seoJson: '{}',
      assetRefsJson: entry.assetRefsJson || '[]',
    })
  }

  /** 加载 DAM 资产目录。 */
  const loadAssetFolders = async () => {
    const response = await marketingContentApi.listAssetFolders()
    setAssetFolders(response.data)
  }

  /** 按搜索表单筛选资产，并在未选择资产时默认选中第一项。 */
  const loadAssets = async (values: AssetSearchValues = assetSearchForm.getFieldsValue()) => {
    // 组装资产查询参数，空文本不传给后端。
    const response = await marketingContentApi.listAssets({
      keyword: trimText(values.keyword),
      assetType: trimText(values.assetType),
      status: trimText(values.status),
    })
    setAssets(response.data)
    if (!selectedAsset && response.data.length > 0) selectAsset(response.data[0])
  }

  /** 按渠道、状态和关键词筛选内容模板。 */
  const loadTemplates = async (values: TemplateSearchValues = templateSearchForm.getFieldsValue()) => {
    // 组装模板查询参数，保持和后端过滤字段一致。
    const response = await marketingContentApi.listTemplates({
      keyword: trimText(values.keyword),
      channel: trimText(values.channel),
      status: trimText(values.status),
    })
    setTemplates(response.data)
    if (!selectedTemplate && response.data.length > 0) selectTemplate(response.data[0])
  }

  /** 按类型、状态和关键词筛选 CMS 内容条目。 */
  const loadEntries = async (values: EntrySearchValues = entrySearchForm.getFieldsValue()) => {
    // 组装内容条目查询参数，空筛选项省略。
    const response = await marketingContentApi.listEntries({
      keyword: trimText(values.keyword),
      contentType: trimText(values.contentType),
      status: trimText(values.status),
    })
    setEntries(response.data)
    if (!selectedEntry && response.data.length > 0) selectEntry(response.data[0])
  }

  /** 加载内容发布快照列表。 */
  const loadReleases = async () => {
    const response = await marketingContentApi.listReleases()
    setReleases(response.data)
  }

  /** 加载发布相关审计事件。 */
  const loadAuditEvents = async () => {
    const response = await marketingContentApi.listAuditEvents({ targetType: 'RELEASE', limit: 20 })
    setAuditEvents(response.data)
  }

  /** 并行加载内容中心首屏所需资产、模板、条目、发布和审计数据。 */
  const loadAll = async () => {
    setLoading(true)
    setError(null)
    try {
      await Promise.all([
        loadAssetFolders(),
        loadAssets(),
        loadTemplates(),
        loadEntries(),
        loadReleases(),
        loadAuditEvents(),
      ])
    } catch (caught) {
      setError((caught as Error).message || '内容中心加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  /** 重置资产编辑器为新建状态。 */
  const newAsset = () => {
    setSelectedAsset(null)
    setSelectedUploadFile(null)
    setUploadIntent(null)
    assetForm.setFieldsValue({
      assetKey: '',
      name: '',
      assetType: 'IMAGE',
      mimeType: 'image/png',
      storageUrl: '',
      folderId: undefined,
      status: 'DRAFT',
      tagsText: '',
      metadataJson: '{}',
      transcodeStatus: undefined,
      durationMs: undefined,
    })
  }

  /** 选择本地文件后，按文件名、MIME 和大小派生资产草稿字段。 */
  const handleUploadFileSelected = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null
    setSelectedUploadFile(file)
    setUploadIntent(null)
    if (!file) return
    const currentValues = assetForm.getFieldsValue()
    const mimeType = file.type || currentValues.mimeType || 'application/octet-stream'
    assetForm.setFieldsValue({
      assetKey: trimText(currentValues.assetKey) ?? assetKeyFromFileName(file.name),
      name: trimText(currentValues.name) ?? file.name,
      assetType: assetTypeFromMime(mimeType),
      mimeType,
      sizeBytes: file.size,
      status: currentValues.status ?? 'DRAFT',
    })
  }

  /** 创建上传意图、直传文件，并把供应商返回的 storageUrl 回填到资产表单。 */
  const handleUploadAssetFile = async () => {
    if (!selectedUploadFile) return
    setUploadingAsset(true)
    setError(null)
    try {
      // 只校验上传必需字段，避免未保存资产的其他字段阻断直传。
      const values = await assetForm.validateFields(['assetKey', 'assetType', 'mimeType'])
      const response = await marketingContentApi.createUploadIntent({
        assetKey: values.assetKey.trim(),
        assetType: values.assetType,
        provider: 'S3',
        mimeType: values.mimeType.trim(),
        fileName: selectedUploadFile.name,
        sizeBytes: selectedUploadFile.size,
      })
      setUploadIntent(response.data)
      await marketingContentApi.uploadAssetFile(response.data, selectedUploadFile)
      const storageUrl = uploadIntentStorageUrl(response.data)
      assetForm.setFieldsValue({
        storageUrl: storageUrl ?? assetForm.getFieldValue('storageUrl'),
        sizeBytes: selectedUploadFile.size,
      })
      message.success('资产文件已上传')
    } catch (caught) {
      setError((caught as Error).message || '资产文件上传失败')
    } finally {
      setUploadingAsset(false)
    }
  }

  /** 重置模板编辑器为新建状态，并提供带变量的示例草稿。 */
  const newTemplate = () => {
    setSelectedTemplate(null)
    setPreview(null)
    templateForm.setFieldsValue({
      templateKey: '',
      displayName: '',
      channel: 'EMAIL',
      subject: 'Hi {{firstName}}',
      body: '<h1>{{headline}}</h1>',
      designJson: '{"blocks":[]}',
      assetRefsJson: '[]',
      status: 'DRAFT',
      previewContextJson: '{"firstName":"Alice","headline":"Welcome"}',
    })
  }

  /** 重置内容条目编辑器为新建状态。 */
  const newEntry = () => {
    setSelectedEntry(null)
    entryForm.setFieldsValue({
      entryKey: '',
      contentType: 'ARTICLE',
      title: '',
      slug: '',
      locale: 'zh-CN',
      summary: '',
      bodyJson: '{"blocks":[]}',
      seoJson: '{}',
      assetRefsJson: '[]',
    })
  }

  /** 保存资产目录。 */
  const handleSaveFolder = async (values: MarketingAssetFolderDraft) => {
    setSavingFolder(true)
    setError(null)
    try {
      await marketingContentApi.saveAssetFolder({
        folderKey: values.folderKey.trim(),
        name: values.name.trim(),
        parentId: values.parentId ?? null,
      })
      folderForm.resetFields()
      message.success('资产目录已保存')
      await loadAssetFolders()
    } catch (caught) {
      setError((caught as Error).message || '资产目录保存失败')
    } finally {
      setSavingFolder(false)
    }
  }

  /** 保存资产草稿，组装标签、元数据和可选媒体字段。 */
  const handleSaveAsset = async (values: AssetEditorValues) => {
    setSavingAsset(true)
    setError(null)
    try {
      // 将编辑器字段转换为后端资产草稿载荷。
      const payload: MarketingAssetDraft = {
        assetKey: values.assetKey.trim(),
        name: values.name.trim(),
        assetType: values.assetType,
        mimeType: values.mimeType.trim(),
        storageUrl: values.storageUrl.trim(),
        folderId: values.folderId ?? undefined,
        sizeBytes: values.sizeBytes ?? undefined,
        checksumSha256: trimText(values.checksumSha256),
        thumbnailUrl: trimText(values.thumbnailUrl),
        posterUrl: trimText(values.posterUrl),
        width: values.width ?? undefined,
        height: values.height ?? undefined,
        durationMs: values.durationMs ?? undefined,
        transcodeStatus: trimText(values.transcodeStatus),
        tags: parseTags(values.tagsText),
        metadata: parseJsonObject(values.metadataJson),
        status: values.status ?? 'DRAFT',
        reviewNotes: trimText(values.reviewNotes),
      }
      const response = await marketingContentApi.saveAsset(payload)
      setSelectedAsset(response.data)
      message.success('资产已保存')
      await loadAssets()
    } catch (caught) {
      setError((caught as Error).message || '资产保存失败')
    } finally {
      setSavingAsset(false)
    }
  }

  /** 更新当前资产审核状态。 */
  const handleAssetStatus = async (status: string) => {
    const assetKey = selectedAsset?.assetKey
    if (!assetKey) return
    setSavingAsset(true)
    setError(null)
    try {
      const response = await marketingContentApi.setAssetStatus(assetKey, { status })
      setSelectedAsset(response.data)
      await loadAssets()
    } catch (caught) {
      setError((caught as Error).message || '资产状态更新失败')
    } finally {
      setSavingAsset(false)
    }
  }

  /** 保存内容模板，校验设计 JSON 和资产引用 JSON。 */
  const handleSaveTemplate = async (values: TemplateEditorValues) => {
    setSavingTemplate(true)
    setError(null)
    try {
      // 组装模板草稿，JSON 字段保持字符串快照供发布固化。
      const payload: ContentTemplateDraft = {
        templateKey: values.templateKey.trim(),
        displayName: values.displayName.trim(),
        channel: values.channel,
        subject: trimText(values.subject),
        body: values.body,
        designJson: normalizeJsonText(values.designJson, '{}'),
        assetRefsJson: normalizeJsonText(values.assetRefsJson, '[]'),
        status: values.status ?? 'DRAFT',
        reviewNotes: trimText(values.reviewNotes),
      }
      const response = await marketingContentApi.saveTemplate(payload)
      setSelectedTemplate(response.data)
      message.success('模板已保存')
      await loadTemplates()
    } catch (caught) {
      setError((caught as Error).message || '模板保存失败')
    } finally {
      setSavingTemplate(false)
    }
  }

  /** 更新当前模板审核状态。 */
  const handleTemplateStatus = async (status: string) => {
    const templateKey = selectedTemplate?.templateKey
    if (!templateKey) return
    setSavingTemplate(true)
    setError(null)
    try {
      const response = await marketingContentApi.setTemplateStatus(templateKey, { status })
      setSelectedTemplate(response.data)
      await loadTemplates()
    } catch (caught) {
      setError((caught as Error).message || '模板状态更新失败')
    } finally {
      setSavingTemplate(false)
    }
  }

  /** 预览模板：已保存模板走后端，未保存草稿走本地变量替换。 */
  const handlePreviewTemplate = async () => {
    const values = templateForm.getFieldsValue()
    setPreviewing(true)
    setError(null)
    try {
      const context = parseJsonObject(values.previewContextJson)
      const response = selectedTemplate?.templateKey
        ? await marketingContentApi.previewTemplate(selectedTemplate.templateKey, context)
        : { data: localTemplatePreview(values.subject ?? '', values.body ?? '', context) }
      setPreview(response.data)
    } catch (caught) {
      setError((caught as Error).message || '模板预览失败')
    } finally {
      setPreviewing(false)
    }
  }

  /** 保存 CMS 内容条目草稿。 */
  const handleSaveEntry = async (values: EntryEditorValues) => {
    setSavingEntry(true)
    setError(null)
    try {
      // 组装内容条目载荷，正文、SEO、资产引用先做 JSON 文本校验。
      const payload: ContentEntryDraft = {
        entryKey: values.entryKey.trim(),
        contentType: values.contentType,
        title: values.title.trim(),
        slug: trimText(values.slug),
        locale: trimText(values.locale),
        summary: trimText(values.summary),
        bodyJson: normalizeJsonText(values.bodyJson, '{}'),
        seoJson: normalizeJsonText(values.seoJson, '{}'),
        assetRefsJson: normalizeJsonText(values.assetRefsJson, '[]'),
      }
      const response = await marketingContentApi.saveEntry(payload)
      setSelectedEntry(response.data)
      message.success('内容草稿已保存')
      await loadEntries()
    } catch (caught) {
      setError((caught as Error).message || '内容保存失败')
    } finally {
      setSavingEntry(false)
    }
  }

  /** 发布或归档当前 CMS 内容条目。 */
  const handleEntryTransition = async (transition: 'publish' | 'archive') => {
    const entryKey = selectedEntry?.entryKey
    if (!entryKey) return
    setSavingEntry(true)
    setError(null)
    try {
      const response = transition === 'publish'
        ? await marketingContentApi.publishEntry(entryKey, {})
        : await marketingContentApi.archiveEntry(entryKey, {})
      setSelectedEntry(response.data)
      await loadEntries()
    } catch (caught) {
      setError((caught as Error).message || '内容状态更新失败')
    } finally {
      setSavingEntry(false)
    }
  }

  /** 从发布表单中读取当前发布来源，并校验 sourceKey 已选择。 */
  const currentReleaseSource = () => {
    const values = releaseForm.getFieldsValue()
    const sourceType = values.sourceType ?? watchedReleaseSourceType
    const sourceKey = trimText(values.sourceKey)
    if (!sourceKey) {
      throw new Error('请选择发布来源')
    }
    return { sourceType, sourceKey }
  }

  /** 调用发布门禁校验，刷新阻断项并清空旧解析结果。 */
  const handleValidateRelease = async () => {
    setReleaseAction('validate')
    setError(null)
    try {
      const source = currentReleaseSource()
      const response = await marketingContentApi.validateRelease(source)
      setReleaseValidation(response.data)
      setResolvedRelease(null)
    } catch (caught) {
      setError((caught as Error).message || '发布门禁校验失败')
    } finally {
      setReleaseAction(null)
    }
  }

  /** 发布当前来源快照，并刷新发布列表和审计事件。 */
  const handlePublishRelease = async () => {
    setReleaseAction('publish')
    setError(null)
    try {
      const source = currentReleaseSource()
      const response = await marketingContentApi.publishRelease({
        ...source,
        note: 'content hub readiness panel',
      })
      setReleases(previous => [response.data, ...previous.filter(release => release.releaseKey !== response.data.releaseKey || release.sourceVersion !== response.data.sourceVersion)])
      message.success('发布快照已生效')
      await Promise.all([loadReleases(), loadAuditEvents()])
    } catch (caught) {
      setError((caught as Error).message || '发布快照失败')
    } finally {
      setReleaseAction(null)
    }
  }

  /** 解析当前 active release 或默认 releaseKey，生成最终内容预览。 */
  const handleResolveRelease = async () => {
    setReleaseAction('resolve')
    setError(null)
    try {
      const source = currentReleaseSource()
      const values = releaseForm.getFieldsValue()
      const response = await marketingContentApi.resolveRelease(
        activeRelease?.releaseKey ?? releaseKeyFor(source.sourceType, source.sourceKey),
        parseJsonObject(values.contextJson),
      )
      setResolvedRelease(response.data)
    } catch (caught) {
      setError((caught as Error).message || '发布快照解析失败')
    } finally {
      setReleaseAction(null)
    }
  }

  /** 回滚当前 active release，并刷新发布列表和审计事件。 */
  const handleRollbackRelease = async () => {
    setReleaseAction('rollback')
    setError(null)
    try {
      if (!activeRelease) {
        throw new Error('当前来源没有生效版本')
      }
      const values = releaseForm.getFieldsValue()
      const response = await marketingContentApi.rollbackRelease(activeRelease.releaseKey, {
        reason: trimText(values.rollbackReason),
      })
      setReleases(previous => [
        response.data,
        ...previous.filter(release => release.releaseKey !== response.data.releaseKey || release.sourceVersion !== response.data.sourceVersion),
      ])
      message.success('发布快照已回滚')
      await Promise.all([loadReleases(), loadAuditEvents()])
    } catch (caught) {
      setError((caught as Error).message || '发布回滚失败')
    } finally {
      setReleaseAction(null)
    }
  }

  const assetColumns: ColumnsType<MarketingAsset> = [
    {
      title: '资产',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Button type="link" style={{ padding: 0 }} onClick={() => selectAsset(row)}>{row.name}</Button>
          <Text type="secondary">{row.assetKey}</Text>
        </Space>
      ),
    },
    { title: '类型', dataIndex: 'assetType', width: 90, render: assetTypeLabel },
    { title: '目录', dataIndex: 'folderId', width: 120, render: value => folderName(value) },
    { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
    {
      title: '视频',
      width: 140,
      render: (_, row) => row.assetType === 'VIDEO'
        ? <Text>{durationLabel(row.durationMs)} / {row.transcodeStatus ?? '-'}</Text>
        : <Text type="secondary">-</Text>,
    },
    {
      title: '标签',
      dataIndex: 'tags',
      render: (tags: string[]) => (
        <Space size={[4, 4]} wrap>
          {tags.length === 0 ? <Text type="secondary">无</Text> : tags.map(tag => <Tag key={tag}>{tag}</Tag>)}
        </Space>
      ),
    },
  ]

  const templateColumns: ColumnsType<ContentTemplate> = [
    {
      title: '模板',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Button type="link" style={{ padding: 0 }} onClick={() => selectTemplate(row)}>{row.displayName}</Button>
          <Text type="secondary">{row.templateKey}</Text>
        </Space>
      ),
    },
    { title: '渠道', dataIndex: 'channel', width: 100, render: channelLabel },
    { title: '状态', dataIndex: 'status', width: 110, render: statusTag },
    {
      title: '变量',
      dataIndex: 'variables',
      render: (variables: string[]) => (
        <Space size={[4, 4]} wrap>
          {variables.length === 0 ? <Text type="secondary">无变量</Text> : variables.map(variable => <Tag key={variable}>{variable}</Tag>)}
        </Space>
      ),
    },
  ]

  const entryColumns: ColumnsType<ContentEntry> = [
    {
      title: '内容',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Button type="link" style={{ padding: 0 }} onClick={() => selectEntry(row)}>{row.title}</Button>
          <Text type="secondary">{row.entryKey}</Text>
        </Space>
      ),
    },
    { title: '类型', dataIndex: 'contentType', width: 130 },
    { title: '语言', dataIndex: 'locale', width: 100, render: value => value || '-' },
    { title: '状态', dataIndex: 'status', width: 110, render: statusTag },
    {
      title: '资产引用',
      dataIndex: 'assetRefsJson',
      width: 110,
      render: value => `${parseAssetRefs(value).length} 个`,
    },
  ]

  /** 渲染生产发布闭环面板，包括门禁、发布、解析和回滚。 */
  const renderReadinessPanel = () => (
    <Card size="small" title="生产发布闭环">
      <Row gutter={[16, 16]}>
        <Col xs={24} xl={14}>
          <Form
            form={releaseForm}
            layout="vertical"
            initialValues={{
              sourceType: 'TEMPLATE',
              contextJson: '{"firstName":"Alice","headline":"Welcome"}',
            }}
            onValuesChange={(changed) => {
              if ('sourceType' in changed) {
                releaseForm.setFieldValue('sourceKey', undefined)
              }
              setReleaseValidation(null)
              setResolvedRelease(null)
            }}
          >
            <Row gutter={12}>
              <Col xs={24} md={8}>
                <Form.Item name="sourceType" label="来源">
                  <Select options={[
                    { value: 'TEMPLATE', label: releaseSourceLabel('TEMPLATE') },
                    { value: 'ENTRY', label: releaseSourceLabel('ENTRY') },
                  ]} />
                </Form.Item>
              </Col>
              <Col xs={24} md={16}>
                <Form.Item name="sourceKey" label="内容 Key" rules={[{ required: true }]}>
                  <Select
                    showSearch
                    placeholder="选择模板或内容"
                    options={releaseSourceOptions}
                    optionFilterProp="label"
                  />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item name="contextJson" label="解析上下文 JSON">
              <Input.TextArea rows={2} />
            </Form.Item>
            <Form.Item name="rollbackReason" label="回滚原因">
              <Input />
            </Form.Item>
            <Space wrap>
              <Button
                icon={<SafetyCertificateOutlined />}
                onClick={handleValidateRelease}
                loading={releaseAction === 'validate'}
                disabled={!watchedReleaseSourceKey}
              >
                校验门禁
              </Button>
              <Button
                type="primary"
                icon={<CloudUploadOutlined />}
                onClick={handlePublishRelease}
                loading={releaseAction === 'publish'}
                disabled={!watchedReleaseSourceKey || releaseValidation?.ready === false}
              >
                发布快照
              </Button>
              <Button
                icon={<EyeOutlined />}
                onClick={handleResolveRelease}
                loading={releaseAction === 'resolve'}
                disabled={!watchedReleaseSourceKey}
              >
                解析预览
              </Button>
              <Button
                danger
                icon={<RollbackOutlined />}
                onClick={handleRollbackRelease}
                loading={releaseAction === 'rollback'}
                disabled={!activeRelease}
              >
                回滚
              </Button>
            </Space>
          </Form>

          <Space direction="vertical" size={12} style={{ width: '100%', marginTop: 16 }}>
            {!releaseValidation && <Tag>未校验</Tag>}
            {releaseValidation?.ready && (
              <Alert
                type="success"
                showIcon
                message="可发布"
                description={`资产引用：${releaseValidation.assetRefs.length} 个`}
              />
            )}
            {releaseValidation && !releaseValidation.ready && (
              <Alert
                type="warning"
                showIcon
                message="存在阻断项"
                description={(
                  <Space direction="vertical" size={4}>
                    {releaseBlockerGroups.map(group => (
                      <Space key={group.scope} direction="vertical" size={2}>
                        <Text strong>{releaseSourceLabel(group.scope)}</Text>
                        {group.blockers.map(blocker => (
                          <Text key={`${blocker.scope}-${blocker.key}-${blocker.reason}`} type="danger">
                            {blocker.key}: {blocker.reason}
                          </Text>
                        ))}
                      </Space>
                    ))}
                  </Space>
                )}
              />
            )}
            {resolvedRelease && (
              <Alert
                type={resolvedRelease.missingVariables.length === 0 ? 'success' : 'warning'}
                showIcon
                message={`解析版本 v${resolvedRelease.sourceVersion}`}
                description={(
                  <Space direction="vertical" size={4} style={{ width: '100%' }}>
                    {resolvedRelease.renderedSubject && <Text strong>{resolvedRelease.renderedSubject}</Text>}
                    {resolvedRelease.renderedBody && (
                      <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{resolvedRelease.renderedBody}</Paragraph>
                    )}
                    <Text type="secondary">{formatMissingVariables(resolvedRelease.missingVariables)}</Text>
                  </Space>
                )}
              />
            )}
          </Space>
        </Col>
        <Col xs={24} xl={10}>
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Space align="center" wrap>
              <Text strong>当前生效</Text>
              {activeRelease
                ? <Tag color={releaseStatusView(activeRelease.status).color}>{releaseStatusView(activeRelease.status).text}</Tag>
                : <Tag>未发布</Tag>}
            </Space>
            {activeRelease ? (
              <Space direction="vertical" size={4}>
                <Text>版本 v{activeRelease.sourceVersion}</Text>
                <Text type="secondary">Key: {activeRelease.releaseKey}</Text>
                <Text type="secondary">Checksum: {activeRelease.checksumSha256.slice(0, 12)}</Text>
              </Space>
            ) : (
              <Text type="secondary">-</Text>
            )}
            <Text strong>审计事件</Text>
            {currentAuditEvents.length === 0 ? (
              <Text type="secondary">暂无审计事件</Text>
            ) : currentAuditEvents.map(event => (
              <Space key={`${event.eventType}-${event.targetKey}-${event.createdAt ?? event.note ?? ''}`} wrap>
                <Tag>{event.eventType}</Tag>
                <Text>{event.actor}</Text>
                {event.note && <Text type="secondary">{event.note}</Text>}
              </Space>
            ))}
          </Space>
        </Col>
      </Row>
    </Card>
  )

  /** 渲染 CMS 内容条目工作区。 */
  const renderEntryTab = () => (
    <Row gutter={[16, 16]}>
      <Col xs={24} xl={14}>
        <Card size="small" title="CMS 条目">
          <Form form={entrySearchForm} layout="inline" onFinish={loadEntries} style={{ marginBottom: 12 }}>
            <Form.Item name="keyword" label="关键词"><Input allowClear placeholder="标题或 Key" /></Form.Item>
            <Form.Item name="contentType" label="类型"><Select allowClear style={{ width: 140 }} options={CONTENT_TYPE_OPTIONS} /></Form.Item>
            <Form.Item name="status" label="状态"><Select allowClear style={{ width: 120 }} options={ENTRY_STATUS_OPTIONS} /></Form.Item>
            <Button htmlType="submit" icon={<SearchOutlined />} loading={loading}>搜索</Button>
          </Form>
          <Table
            rowKey="entryKey"
            columns={entryColumns}
            dataSource={entries}
            loading={loading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无内容条目" /> }}
          />
        </Card>
      </Col>
      <Col xs={24} xl={10}>
        <Card size="small" title={selectedEntry ? '编辑内容' : '新建内容'}>
          <Form
            form={entryForm}
            layout="vertical"
            initialValues={{
              contentType: 'ARTICLE',
              locale: 'zh-CN',
              bodyJson: '{"blocks":[]}',
              seoJson: '{}',
              assetRefsJson: '[]',
            }}
            onFinish={handleSaveEntry}
          >
            <Row gutter={12}>
              <Col xs={24} md={12}>
                <Form.Item name="entryKey" label="内容 Key" rules={[{ required: true }]}>
                  <Input disabled={Boolean(selectedEntry)} placeholder="spring_guide" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="contentType" label="类型" rules={[{ required: true }]}>
                  <Select options={CONTENT_TYPE_OPTIONS} />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item name="title" label="标题" rules={[{ required: true }]}><Input /></Form.Item>
            <Row gutter={12}>
              <Col xs={24} md={12}><Form.Item name="slug" label="Slug"><Input /></Form.Item></Col>
              <Col xs={24} md={12}><Form.Item name="locale" label="语言"><Input /></Form.Item></Col>
            </Row>
            <Form.Item name="summary" label="摘要"><Input.TextArea rows={2} /></Form.Item>
            <Form.Item name="bodyJson" label="正文 JSON" rules={[{ required: true }]}><Input.TextArea rows={5} /></Form.Item>
            <Form.Item name="seoJson" label="SEO JSON"><Input.TextArea rows={3} /></Form.Item>
            <Form.Item name="assetRefsJson" label="资产引用 JSON"><Input.TextArea rows={2} /></Form.Item>
            <Space wrap>
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={savingEntry}>保存草稿</Button>
              <Button icon={<CheckCircleOutlined />} disabled={!selectedEntry} onClick={() => handleEntryTransition('publish')} loading={savingEntry}>发布</Button>
              <Button disabled={!selectedEntry} onClick={() => handleEntryTransition('archive')} loading={savingEntry}>归档</Button>
              <Button onClick={newEntry}>新建</Button>
            </Space>
          </Form>
        </Card>
      </Col>
    </Row>
  )

  /** 渲染 DAM 资产工作区。 */
  const renderAssetTab = () => (
    <Row gutter={[16, 16]}>
      <Col xs={24} xl={14}>
        <Card size="small" title="资产目录" style={{ marginBottom: 16 }}>
          <Form
            form={folderForm}
            layout="inline"
            onFinish={handleSaveFolder}
            initialValues={{ parentId: null }}
            style={{ marginBottom: 12 }}
          >
            <Form.Item name="folderKey" label="目录 Key" rules={[{ required: true }]}>
              <Input placeholder="campaign_assets" />
            </Form.Item>
            <Form.Item name="name" label="目录名称" rules={[{ required: true }]}>
              <Input placeholder="Campaign Assets" />
            </Form.Item>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={savingFolder}>保存目录</Button>
          </Form>
          <Space size={[4, 4]} wrap>
            {assetFolders.length === 0 ? <Text type="secondary">暂无资产目录</Text> : assetFolders.map(folder => (
              <Tag key={folder.id}>{folder.name}</Tag>
            ))}
          </Space>
        </Card>
        <Card size="small" title="DAM 资产">
          <Form form={assetSearchForm} layout="inline" onFinish={loadAssets} style={{ marginBottom: 12 }}>
            <Form.Item name="keyword" label="关键词"><Input allowClear placeholder="名称或 Key" /></Form.Item>
            <Form.Item name="assetType" label="类型"><Select allowClear style={{ width: 120 }} options={ASSET_TYPE_OPTIONS} /></Form.Item>
            <Form.Item name="status" label="状态"><Select allowClear style={{ width: 120 }} options={ASSET_STATUS_OPTIONS} /></Form.Item>
            <Button htmlType="submit" icon={<SearchOutlined />} loading={loading}>搜索</Button>
          </Form>
          <Table
            rowKey="assetKey"
            columns={assetColumns}
            dataSource={assets}
            loading={loading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无资产" /> }}
          />
        </Card>
      </Col>
      <Col xs={24} xl={10}>
        <Card size="small" title={selectedAsset ? '编辑资产' : '新建资产'}>
          <Form
            form={assetForm}
            layout="vertical"
            initialValues={{ assetType: 'IMAGE', status: 'DRAFT', metadataJson: '{}' }}
            onFinish={handleSaveAsset}
          >
            <Row gutter={12}>
              <Col xs={24} md={12}>
                <Form.Item name="assetKey" label="资产 Key" rules={[{ required: true }]}>
                  <Input disabled={Boolean(selectedAsset)} placeholder="launch_video" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="assetType" label="类型" rules={[{ required: true }]}>
                  <Select options={ASSET_TYPE_OPTIONS} />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item name="name" label="资产名称" rules={[{ required: true }]}><Input /></Form.Item>
            <Row gutter={12}>
              <Col xs={24} md={12}><Form.Item name="mimeType" label="MIME" rules={[{ required: true }]}><Input /></Form.Item></Col>
              <Col xs={24} md={12}><Form.Item name="status" label="状态"><Select options={ASSET_STATUS_OPTIONS} /></Form.Item></Col>
            </Row>
            <Form.Item label="文件上传">
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Input aria-label="文件上传" type="file" onChange={handleUploadFileSelected} />
                <Space wrap>
                  <Button
                    icon={<CloudUploadOutlined />}
                    onClick={handleUploadAssetFile}
                    loading={uploadingAsset}
                    disabled={!selectedUploadFile}
                  >
                    上传文件
                  </Button>
                  {uploadIntent && <Tag>{uploadIntent.status}</Tag>}
                </Space>
              </Space>
            </Form.Item>
            <Form.Item name="storageUrl" label="文件 URL" rules={[{ required: true, type: 'url' }]}><Input /></Form.Item>
            <Form.Item name="folderId" label="资产目录">
              <Select allowClear options={folderOptions} placeholder="选择目录" />
            </Form.Item>
            <Row gutter={12}>
              <Col xs={24} md={12}><Form.Item name="posterUrl" label="视频封面 URL"><Input /></Form.Item></Col>
              <Col xs={24} md={12}><Form.Item name="transcodeStatus" label="转码状态"><Select allowClear options={TRANSCODE_STATUS_OPTIONS} /></Form.Item></Col>
            </Row>
            <Row gutter={12}>
              <Col xs={24} md={8}><Form.Item name="durationMs" label="时长 ms"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
              <Col xs={24} md={8}><Form.Item name="width" label="宽度"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
              <Col xs={24} md={8}><Form.Item name="height" label="高度"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
            </Row>
            <Form.Item name="tagsText" label="标签"><Input placeholder="campaign, hero" /></Form.Item>
            <Form.Item name="metadataJson" label="元数据 JSON"><Input.TextArea rows={3} /></Form.Item>
            <Form.Item name="reviewNotes" label="审核备注"><Input.TextArea rows={2} /></Form.Item>
            <Space wrap>
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={savingAsset}>保存资产</Button>
              <Button disabled={!selectedAsset} onClick={() => handleAssetStatus('READY')} loading={savingAsset}>设为可用</Button>
              <Button disabled={!selectedAsset} onClick={() => handleAssetStatus('ARCHIVED')} loading={savingAsset}>归档</Button>
              <Button onClick={newAsset}>新建</Button>
            </Space>
          </Form>
        </Card>
      </Col>
    </Row>
  )

  /** 渲染设计模板工作区。 */
  const renderTemplateTab = () => (
    <Row gutter={[16, 16]}>
      <Col xs={24} xl={14}>
        <Card size="small" title="邮件模板">
          <Form form={templateSearchForm} layout="inline" onFinish={loadTemplates} style={{ marginBottom: 12 }}>
            <Form.Item name="keyword" label="关键词"><Input allowClear placeholder="名称或 Key" /></Form.Item>
            <Form.Item name="channel" label="渠道"><Select allowClear style={{ width: 120 }} options={CHANNEL_OPTIONS} /></Form.Item>
            <Form.Item name="status" label="状态"><Select allowClear style={{ width: 130 }} options={TEMPLATE_STATUS_OPTIONS} /></Form.Item>
            <Button htmlType="submit" icon={<SearchOutlined />} loading={loading}>搜索</Button>
          </Form>
          <Table
            rowKey="templateKey"
            columns={templateColumns}
            dataSource={templates}
            loading={loading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无内容模板" /> }}
          />
        </Card>
      </Col>
      <Col xs={24} xl={10}>
        <Card size="small" title={selectedTemplate ? '编辑设计模板' : '新建设计模板'}>
          <Form
            form={templateForm}
            layout="vertical"
            initialValues={{
              channel: 'EMAIL',
              subject: 'Hi {{firstName}}',
              body: '<h1>{{headline}}</h1>',
              designJson: '{"blocks":[]}',
              assetRefsJson: '[]',
              status: 'DRAFT',
              previewContextJson: '{"firstName":"Alice","headline":"Welcome"}',
            }}
            onFinish={handleSaveTemplate}
          >
            <Row gutter={12}>
              <Col xs={24} md={12}>
                <Form.Item name="templateKey" label="模板 Key" rules={[{ required: true }]}>
                  <Input disabled={Boolean(selectedTemplate)} placeholder="welcome_email" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="channel" label="渠道" rules={[{ required: true }]}>
                  <Select options={CHANNEL_OPTIONS} />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item name="displayName" label="模板名称" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item name="subject" label="主题"><Input /></Form.Item>
            <Form.Item name="body" label="HTML / 文案" rules={[{ required: true }]}><Input.TextArea rows={5} /></Form.Item>
            <Space size={[4, 4]} wrap style={{ marginBottom: 12 }}>
              {draftVariables.length === 0 ? <Text type="secondary">当前模板无变量</Text> : draftVariables.map(variable => <Tag key={variable}>{variable}</Tag>)}
            </Space>
            <Form.Item name="designJson" label="设计 JSON"><Input.TextArea rows={3} /></Form.Item>
            <Form.Item name="assetRefsJson" label="资产引用 JSON"><Input.TextArea rows={2} /></Form.Item>
            <Row gutter={12}>
              <Col xs={24} md={12}><Form.Item name="status" label="状态"><Select options={TEMPLATE_STATUS_OPTIONS} /></Form.Item></Col>
              <Col xs={24} md={12}><Form.Item name="previewContextJson" label="预览上下文 JSON"><Input.TextArea rows={2} /></Form.Item></Col>
            </Row>
            <Form.Item name="reviewNotes" label="审核备注"><Input.TextArea rows={2} /></Form.Item>
            <Space wrap>
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={savingTemplate}>保存模板</Button>
              <Button onClick={handlePreviewTemplate} loading={previewing}>预览</Button>
              <Button disabled={!selectedTemplate} onClick={() => handleTemplateStatus('PENDING_APPROVAL')} loading={savingTemplate}>提审</Button>
              <Button disabled={!selectedTemplate} onClick={() => handleTemplateStatus('APPROVED')} loading={savingTemplate}>通过</Button>
              <Button onClick={newTemplate}>新建</Button>
            </Space>
          </Form>
          {preview && (
            <Alert
              style={{ marginTop: 16 }}
              type={preview.missingVariables.length === 0 ? 'success' : 'warning'}
              showIcon
              message={formatMissingVariables(preview.missingVariables)}
              description={(
                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                  {preview.renderedSubject && <Text strong>{preview.renderedSubject}</Text>}
                  <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{preview.renderedBody}</Paragraph>
                </Space>
              )}
            />
          )}
        </Card>
      </Col>
    </Row>
  )

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
        <Title level={3} style={{ margin: 0 }}>内容中心</Title>
        <Button icon={<ReloadOutlined />} onClick={loadAll} loading={loading}>刷新</Button>
      </Space>

      {error && <Alert type="error" showIcon message={error} />}

      <Row gutter={[16, 16]}>
        <Col xs={24} md={6}>
          <Card size="small"><Statistic title="CMS 条目" value={entries.length} prefix={<FileTextOutlined />} /></Card>
        </Col>
        <Col xs={24} md={6}>
          <Card size="small"><Statistic title="视频资产" value={assets.filter(asset => asset.assetType === 'VIDEO').length} prefix={<VideoCameraOutlined />} /></Card>
        </Col>
        <Col xs={24} md={6}>
          <Card size="small">
            <Statistic title="资产目录" value={assetFolders.length} prefix={<FolderOpenOutlined />} />
            <Space size={[4, 4]} wrap style={{ marginTop: 8 }}>
              {assetFolders.slice(0, 3).map(folder => <Tag key={folder.id}>{folder.name}</Tag>)}
            </Space>
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card size="small"><Statistic title="邮件模板" value={templates.filter(template => template.channel === 'EMAIL').length} prefix={<PictureOutlined />} /></Card>
        </Col>
      </Row>

      {renderReadinessPanel()}

      <Tabs
        defaultActiveKey="entries"
        items={[
          { key: 'entries', label: '内容条目', children: renderEntryTab() },
          { key: 'assets', label: '资产库', children: renderAssetTab() },
          { key: 'templates', label: '设计模板', children: renderTemplateTab() },
        ]}
      />
    </Space>
  )
}
