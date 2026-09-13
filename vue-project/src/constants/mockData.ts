import {
  BookOpen,
  CalendarDays,
  Database,
  GraduationCap,
  LayoutDashboard,
  MessageSquare,
  Settings,
  ShieldCheck,
  UsersRound,
} from 'lucide-vue-next';
import type { ChatMessage, ChatSession, KnowledgeBase, NavGroup, QuickQuestion } from '../types';

export const navGroups: NavGroup[] = [
  {
    id: 'qa',
    title: '智能服务',
    items: [
      { id: 'assistant', label: '智能问答', description: '校园政策与办事咨询', icon: MessageSquare },
      { id: 'student', label: '学生服务', description: '选课、成绩、资助', icon: GraduationCap },
      { id: 'teacher', label: '教师服务', description: '科研、人事、教学', icon: UsersRound },
    ],
  },
  {
    id: 'knowledge',
    title: '知识中枢',
    items: [
      { id: 'kb', label: '知识库管理', description: '文档向量化与更新', icon: Database },
      { id: 'guide', label: '政策指南', description: '校内制度快速检索', icon: BookOpen },
      { id: 'calendar', label: '校历安排', description: '教学周与考试节点', icon: CalendarDays },
    ],
  },
  {
    id: 'system',
    title: '系统',
    items: [
      { id: 'dashboard', label: '数据看板', description: '问答质量与使用趋势', icon: LayoutDashboard },
      { id: 'security', label: '权限审计', description: '访问范围与数据安全', icon: ShieldCheck },
      { id: 'settings', label: '系统设置', description: '模型、语音与接口', icon: Settings },
    ],
  },
];

export const knowledgeBases: KnowledgeBase[] = [
  {
    id: 1,
    name: '本科教学管理制度',
    category: '教务',
    status: 'ready',
    documents: 86,
    updatedAt: '今天 09:18',
    description: '培养方案、学籍异动、选课退课、考试安排、成绩复核。',
  },
  {
    id: 2,
    name: '研究生培养与学位',
    category: '研究生院',
    status: 'ready',
    documents: 52,
    updatedAt: '昨天 18:40',
    description: '开题、中期、论文送审、答辩流程和学位申请材料。',
  },
  {
    id: 3,
    name: '智慧校园办事指南',
    category: '综合服务',
    status: 'syncing',
    documents: 123,
    updatedAt: '同步中',
    description: '一卡通、宿舍、网络、图书馆、场馆预约与后勤报修。',
  },
  {
    id: 4,
    name: '招生就业政策库',
    category: '招生就业',
    status: 'ready',
    documents: 41,
    updatedAt: '周一 14:06',
    description: '本科招生、转专业、就业派遣、实习协议和双选会。',
  },
  {
    id: 5,
    name: '科研项目与经费',
    category: '科研',
    status: 'review',
    documents: 34,
    updatedAt: '待复核',
    description: '项目申报、经费报销、成果登记、实验室安全规范。',
  },
];

export const chatSessions: ChatSession[] = [
  { id: 1, title: '补考与缓考申请', scope: '本科教学管理制度', messageCount: 8, updatedAt: '3 分钟前', pinned: true },
  { id: 2, title: '毕业论文送审材料', scope: '研究生培养与学位', messageCount: 12, updatedAt: '今天 10:31' },
  { id: 3, title: '校园网账号问题', scope: '智慧校园办事指南', messageCount: 5, updatedAt: '昨天 21:18' },
  { id: 4, title: '就业协议盖章流程', scope: '招生就业政策库', messageCount: 7, updatedAt: '周二 16:42' },
];

export const initialMessages: ChatMessage[] = [
  {
    id: 1,
    role: 'assistant',
    content: '你好，我是青岛农业大学智能问答助手。可以帮你检索校内制度、办事流程、教学安排和常见问题。',
    time: '09:20',
    sources: ['智慧校园办事指南'],
  },
  {
    id: 2,
    role: 'user',
    content: '如果因为生病错过期末考试，应该怎么申请缓考？',
    time: '09:21',
  },
  {
    id: 3,
    role: 'assistant',
    content: '一般需要在规定时间内提交缓考申请，并附医院诊断证明等材料。建议先联系所在学院教学秘书确认截止时间，再通过教务系统或学院指定流程提交申请。',
    time: '09:21',
    sources: ['本科教学管理制度', '智慧校园办事指南'],
  },
];

export const quickQuestions: QuickQuestion[] = [
  { id: 1, label: '缓考申请', question: '因病错过考试，缓考申请需要哪些材料？' },
  { id: 2, label: '转专业', question: '本科生申请转专业通常需要满足哪些条件？' },
  { id: 3, label: '奖助学金', question: '国家励志奖学金和助学金的申请流程是什么？' },
  { id: 4, label: '校园卡', question: '校园卡丢失后如何挂失和补办？' },
  { id: 5, label: '图书馆', question: '图书馆借阅超期后怎么处理？' },
];
