-- C:\Users\艾米\IdeaProjects\QAU-smartAI\qau-infrastructure\src\main\resources\db\migration\V4__seed_demo_data.sql

-- ============================================
-- 种子数据：让项目开箱即用的完整演示数据
-- ============================================

-- 1. 知识库（5 个，对应前端 Mock）
INSERT INTO knowledge_base (name, category, status, description, document_count, owner_department, last_synced_at, created_by, created_at, updated_at, deleted) VALUES
('本科教学管理制度', '教务', 'ready', '培养方案、学籍异动、选课退课、考试安排、成绩复核。', 86, '教务处', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('研究生培养与学位', '研究生院', 'ready', '开题、中期、论文送审、答辩流程和学位申请材料。', 52, '研究生院', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('智慧校园办事指南', '综合服务', 'syncing', '一卡通、宿舍、网络、图书馆、场馆预约与后勤报修。', 123, '信息中心', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('招生就业政策库', '招生就业', 'ready', '本科招生、转专业、就业派遣、实习协议和双选会。', 41, '招生就业处', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('科研项目与经费', '科研', 'review', '项目申报、经费报销、成果登记、实验室安全规范。', 34, '科研处', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- 2. 导航分组（3 个）
INSERT INTO nav_group (group_key, title, sort_order, created_at, updated_at, deleted) VALUES
                                                                                          ('qa', '智能服务', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                          ('knowledge', '知识中枢', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                          ('system', '系统', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- 3. 导航菜单项（9 个）
INSERT INTO nav_item (group_id, item_key, label, description, icon_name, route_path, sort_order, created_at, updated_at, deleted) VALUES
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'qa'), 'assistant', '智能问答', '校园政策与办事咨询', 'MessageSquare', '/assistant', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'qa'), 'student', '学生服务', '选课、成绩、资助', 'GraduationCap', '/student', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'qa'), 'teacher', '教师服务', '科研、人事、教学', 'UsersRound', '/teacher', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'knowledge'), 'kb', '知识库管理', '文档向量化与更新', 'Database', '/knowledge', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'knowledge'), 'guide', '政策指南', '校内制度快速检索', 'BookOpen', '/guide', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'knowledge'), 'calendar', '校历安排', '教学周与考试节点', 'CalendarDays', '/calendar', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'system'), 'dashboard', '数据看板', '问答质量与使用趋势', 'LayoutDashboard', '/dashboard', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'system'), 'security', '权限审计', '访问范围与数据安全', 'ShieldCheck', '/security', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                                                                      ((SELECT id FROM nav_group WHERE group_key = 'system'), 'settings', '系统设置', '模型、语音与接口', 'Settings', '/settings', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- 4. 快捷问题（5 个）
INSERT INTO quick_question (label, question, sort_order, created_at, updated_at, deleted) VALUES
                                                                                              ('缓考申请', '因病错过考试，缓考申请需要哪些材料？', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                              ('转专业', '本科生申请转专业通常需要满足哪些条件？', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                              ('奖助学金', '国家励志奖学金和助学金的申请流程是什么？', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                              ('校园卡', '校园卡丢失后如何挂失和补办？', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
                                                                                              ('图书馆', '图书馆借阅超期后怎么处理？', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- 5. 默认管理员账号（密码：admin123，BCrypt 加密）
INSERT INTO user_account (username, real_name, password_hash, role_code, status, created_at, updated_at, deleted) VALUES
    ('admin', '系统管理员', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);