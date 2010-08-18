/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2008 Aimluck,Inc.
 * http://aipostyle.com/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.EipTSchedule;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.cayenne.om.portlet.EipTTodo;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.DatabaseOrmService;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.todo.util.ToDoUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * 週間スケジュールの検索結果を管理するクラスです。
 * 
 */
public class ScheduleWeeklySelectData extends
    ALAbstractSelectData<EipTScheduleMap> {

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ScheduleWeeklySelectData.class.getName());

  /** <code>prevDate</code> 前の日 */
  private ALDateTimeField prevDate;

  /** <code>nextDate</code> 次の日 */
  private ALDateTimeField nextDate;

  /** <code>prevWeek</code> 前の週 */
  private ALDateTimeField prevWeek;

  /** <code>nextWeek</code> 次の週 */
  private ALDateTimeField nextWeek;

  /** <code>today</code> 今日 */
  private ALDateTimeField today;

  /** <code>prevMonth</code> 前の月 */
  private ALDateTimeField prevMonth;

  /** <code>nextMonth</code> 次の月 */
  private ALDateTimeField nextMonth;

  /** <code>viewStart</code> 表示開始日時 */
  private ALDateTimeField viewStart;

  /** <code>viewEnd</code> 表示終了日時 */
  private ALDateTimeField viewEnd;

  /** <code>viewEndCrt</code> 表示終了日時 (Criteria) */
  private ALDateTimeField viewEndCrt;

  /** <code>weekTermConList</code> 期間スケジュールリスト */
  private List<ScheduleTermWeekContainer> termWeekConList;

  /** <code>weekCon</code> 週間スケジュールコンテナ */
  private ScheduleWeekContainer weekCon;

  /** <code>viewtype</code> 表示タイプ */
  protected String viewtype;

  /** <code>tmpCal</code> テンポラリ日付 */
  protected Calendar tmpCal;

  /** <code>weekTodoConList</code> ToDo リスト（週間スケジュール用） */
  private List<ScheduleToDoWeekContainer> weekTodoConList;

  /** <code>viewJob</code> ToDo 表示設定 */
  protected int viewTodo;

  /** ポートレット ID */
  private String portletId;

  protected DataContext dataContext;

  /** <code>hasAuthoritySelfInsert</code> アクセス権限 */
  private boolean hasAuthoritySelfInsert = false;

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractSelectData#init(com.aimluck.eip.modules
   * .actions.common.ALAction, org.apache.turbine.util.RunData,
   * org.apache.velocity.context.Context)
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    // 展開されるパラメータは以下の通りです。
    // ・viewStart 形式：yyyy-MM-dd

    // 表示タイプの設定
    viewtype = "weekly";
    // POST/GET から yyyy-MM-dd の形式で受け渡される。
    // 前の日
    prevDate = new ALDateTimeField("yyyy-MM-dd");
    // 次の日
    nextDate = new ALDateTimeField("yyyy-MM-dd");
    // 前の週
    prevWeek = new ALDateTimeField("yyyy-MM-dd");
    // 次の週
    nextWeek = new ALDateTimeField("yyyy-MM-dd");
    // 前の月
    prevMonth = new ALDateTimeField("yyyy-MM-dd");
    // 次の月
    nextMonth = new ALDateTimeField("yyyy-MM-dd");
    // 表示開始日時
    viewStart = new ALDateTimeField("yyyy-MM-dd");
    viewStart.setNotNull(true);
    // 表示終了日時
    viewEnd = new ALDateTimeField("yyyy-MM-dd");
    // 表示終了日時 (Criteria)
    viewEndCrt = new ALDateTimeField("yyyy-MM-dd");
    // 今日
    today = new ALDateTimeField("yyyy-MM-dd");
    Calendar to = Calendar.getInstance();
    to.set(Calendar.HOUR_OF_DAY, 0);
    to.set(Calendar.MINUTE, 0);
    today.setValue(to.getTime());

    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // スケジュールの表示開始日時
      // e.g. 2004-3-14
      if (rundata.getParameters().containsKey("view_start")) {
        ALEipUtils.setTemp(rundata, context, "view_start", rundata
          .getParameters().getString("view_start"));
      }
    }

    // 表示開始日時
    String tmpViewStart = ALEipUtils.getTemp(rundata, context, "view_start");
    if (tmpViewStart == null || tmpViewStart.equals("")) {
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      viewStart.setValue(cal.getTime());
    } else {
      viewStart.setValue(tmpViewStart);
      if (!viewStart.validate(new ArrayList<String>())) {
        ALEipUtils.removeTemp(rundata, context, "view_start");
        throw new ALPageNotFoundException();
      }
    }
    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(viewStart.getValue());
    cal2.add(Calendar.DATE, 1);
    nextDate.setValue(cal2.getTime());
    cal2.add(Calendar.DATE, 6);
    nextWeek.setValue(cal2.getTime());
    cal2.add(Calendar.DATE, -8);
    prevDate.setValue(cal2.getTime());
    cal2.add(Calendar.DATE, -6);
    prevWeek.setValue(cal2.getTime());
    cal2.add(Calendar.DATE, 7);
    // このときの日付を捕捉
    tmpCal = Calendar.getInstance();
    tmpCal.setTime(cal2.getTime());
    // 週間スケジュールコンテナの初期化
    try {
      weekCon = new ScheduleWeekContainer();
      weekCon.initField();
      weekCon.setViewStartDate(cal2);
    } catch (Exception e) {
      logger.error("Exception", e);
    }
    // 表示終了日時
    viewEndCrt.setValue(cal2.getTime());
    cal2.add(Calendar.DATE, -1);
    viewEnd.setValue(cal2.getTime());

    Calendar cal3 = Calendar.getInstance();
    cal3.setTime(viewStart.getValue());
    cal3.add(Calendar.MONTH, -1);
    prevMonth.setValue(cal3.getTime());
    cal3.add(Calendar.MONTH, 2);
    nextMonth.setValue(cal3.getTime());

    ALEipUtils.setTemp(rundata, context, "tmpStart", viewStart.toString()
      + "-00-00");
    ALEipUtils.setTemp(rundata, context, "tmpEnd", viewStart.toString()
      + "-00-00");

    termWeekConList = new ArrayList<ScheduleTermWeekContainer>();
    weekTodoConList = new ArrayList<ScheduleToDoWeekContainer>();

    if (action != null) {
      // ToDo 表示設定
      viewTodo = Integer.parseInt(ALEipUtils.getPortlet(rundata, context)
        .getPortletConfig().getInitParameter("p5a-view"));
    }
    dataContext = DatabaseOrmService.getInstance().getDataContext();

    // スーパークラスのメソッドを呼び出す。
    super.init(action, rundata, context);

    int userId = ALEipUtils.getUserId(rundata);

    // アクセス権限
    ALAccessControlFactoryService aclservice = (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
      .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAuthoritySelfInsert = aclhandler.hasAuthority(userId,
      ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF,
      ALAccessControlConstants.VALUE_ACL_INSERT);
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractSelectData#selectList(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context)
   */
  @Override
  protected List<EipTScheduleMap> selectList(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    try {

      List<EipTScheduleMap> list = getSelectQuery(rundata, context).perform();

      if (viewTodo == 1) {
        // ToDo の読み込み
        loadTodo(rundata, context);
      }

      return ScheduleUtils.sortByDummySchedule(list);
    } catch (Exception e) {
      logger.error("[ScheduleWeeklySelectData] TorqueException");
      throw new ALDBErrorException();

    }
  }

  /**
   * 検索条件を設定した SelectQuery を返します。
   * 
   * @param rundata
   * @param context
   * @return
   */
  protected SelectQuery<EipTScheduleMap> getSelectQuery(RunData rundata,
      Context context) {
    SelectQuery<EipTScheduleMap> query = Database.query(EipTScheduleMap.class);

    // 自ユーザ
    Expression exp1 = ExpressionFactory.matchExp(
      EipTScheduleMap.USER_ID_PROPERTY,
      Integer.valueOf(ALEipUtils.getUserId(rundata)));
    query.setQualifier(exp1);
    // ユーザのスケジュール
    Expression exp2 = ExpressionFactory.matchExp(EipTScheduleMap.TYPE_PROPERTY,
      ScheduleUtils.SCHEDULEMAP_TYPE_USER);
    query.andQualifier(exp2);

    // 終了日時
    Expression exp11 = ExpressionFactory.greaterOrEqualExp(
      EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
        + EipTSchedule.END_DATE_PROPERTY, viewStart.getValue());
    // 開始日時
    Expression exp12 = ExpressionFactory.lessOrEqualExp(
      EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
        + EipTSchedule.START_DATE_PROPERTY, viewEndCrt.getValue());
    // 通常スケジュール
    Expression exp13 = ExpressionFactory.noMatchExp(
      EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
        + EipTSchedule.REPEAT_PATTERN_PROPERTY, "N");
    // 期間スケジュール
    Expression exp14 = ExpressionFactory.noMatchExp(
      EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
        + EipTSchedule.REPEAT_PATTERN_PROPERTY, "S");
    query.andQualifier((exp11.andExp(exp12)).orExp(exp13.andExp(exp14)));
    // 開始日時でソート
    query.orderAscending(EipTScheduleMap.EIP_TSCHEDULE_PROPERTY + "."
      + EipTSchedule.START_DATE_PROPERTY);

    return query;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractSelectData#getResultData(java.lang.Object)
   */
  @Override
  protected Object getResultData(EipTScheduleMap record)
      throws ALPageNotFoundException, ALDBErrorException {
    ScheduleResultData rd = new ScheduleResultData();
    rd.initField();
    try {
      EipTSchedule schedule = record.getEipTSchedule();
      // スケジュールが棄却されている場合は表示しない
      if ("R".equals(record.getStatus())) {
        return rd;
      }
      // ID
      rd.setScheduleId(schedule.getScheduleId().intValue());
      // 親スケジュール ID
      rd.setParentId(schedule.getParentId().intValue());
      // 名前
      rd.setName(schedule.getName());
      // 開始日時
      rd.setStartDate(schedule.getStartDate());
      // 終了日時
      rd.setEndDate(schedule.getEndDate());
      // 仮スケジュールかどうか
      rd.setTmpreserve("T".equals(record.getStatus()));
      // 公開するかどうか
      rd.setPublic("O".equals(schedule.getPublicFlag()));
      // 非表示にするかどうか
      rd.setHidden("P".equals(schedule.getPublicFlag()));
      // ダミーか
      rd.setDummy("D".equals(record.getStatus()));
      // 繰り返しパターン
      rd.setPattern(schedule.getRepeatPattern());

      // 期間スケジュールの場合
      if (rd.getPattern().equals("S")) {
        int stime = -(int) ((viewStart.getValue().getTime() - rd.getStartDate()
          .getValue().getTime()) / 86400000);
        int etime = -(int) ((viewStart.getValue().getTime() - rd.getEndDate()
          .getValue().getTime()) / 86400000);
        if (stime < 0) {
          stime = 0;
        }
        int count = stime;
        int col = etime - stime + 1;
        // 行をはみ出す場合
        if (count + col > 7) {
          col = 7 - count;
        }
        // rowspan を設定
        rd.setRowspan(col);
        if (col > 0) {
          // 期間スケジュール を格納
          ScheduleUtils.addTermSchedule(termWeekConList, viewStart.getValue(),
            count, rd);
        }

        return rd;
      }
      // スケジュールをコンテナに格納
      weekCon.addResultData(rd);
    } catch (Exception e) {

      // TODO: エラー処理
      logger.error("Exception", e);

      return null;
    }
    return rd;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractSelectData#selectDetail(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context)
   */
  @Override
  protected EipTScheduleMap selectDetail(RunData rundata, Context context) {
    return null;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractSelectData#getResultDataDetail(java.lang
   * .Object)
   */
  @Override
  protected Object getResultDataDetail(EipTScheduleMap obj) {
    return null;
  }

  /*
   * @see com.aimluck.eip.common.ALAbstractSelectData#getColumnMap()
   */
  @Override
  protected Attributes getColumnMap() {
    // このメソッドは利用されません。
    return null;
  }

  public void loadTodo(RunData rundata, Context context) {
    try {
      SelectQuery<EipTTodo> query = getSelectQueryForTodo(rundata, context);
      List<EipTTodo> todos = query.perform();

      int todossize = todos.size();
      for (int i = 0; i < todossize; i++) {
        EipTTodo record = todos.get(i);
        ScheduleToDoResultData rd = new ScheduleToDoResultData();
        rd.initField();

        // ポートレット ToDoPublic のへのリンクを取得する．
        String todo_url = ScheduleUtils.getPortletURItoTodoDetailPane(rundata,
          "ToDo", record.getTodoId().longValue(), portletId);
        rd.setTodoId(record.getTodoId().intValue());
        rd.setTodoName(record.getTodoName());
        rd.setUserId(record.getTurbineUser().getUserId().intValue());
        rd.setStartDate(record.getStartDate());
        rd.setEndDate(record.getEndDate());
        rd.setTodoUrl(todo_url);
        // 公開/非公開を設定する．
        rd.setPublicFlag("T".equals(record.getPublicFlag()));

        int stime;
        if (ScheduleUtils.equalsToDate(ToDoUtils.getEmptyDate(), rd
          .getStartDate().getValue(), false)) {
          stime = 0;
        } else {
          stime = -(int) ((viewStart.getValue().getTime() - rd.getStartDate()
            .getValue().getTime()) / 86400000);
        }
        int etime = -(int) ((viewStart.getValue().getTime() - rd.getEndDate()
          .getValue().getTime()) / 86400000);
        if (stime < 0) {
          stime = 0;
        }
        int count = stime;
        int col = etime - stime + 1;
        // 行をはみ出す場合
        if (count + col > 7) {
          col = 7 - count;
        }

        // rowspan を設定
        rd.setRowspan(col);
        if (col > 0) {
          // ToDo を格納
          ScheduleUtils.addToDo(weekTodoConList, viewStart.getValue(), count,
            rd);
        }
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return;
    }
  }

  private SelectQuery<EipTTodo> getSelectQueryForTodo(RunData rundata,
      Context context) {
    Integer uid = Integer.valueOf(ALEipUtils.getUserId(rundata));
    SelectQuery<EipTTodo> query = new SelectQuery<EipTTodo>(EipTTodo.class);

    Expression exp1 = ExpressionFactory.noMatchExp(EipTTodo.STATE_PROPERTY,
      Short.valueOf((short) 100));
    query.setQualifier(exp1);
    Expression exp2 = ExpressionFactory.matchExp(
      EipTTodo.ADDON_SCHEDULE_FLG_PROPERTY, "T");
    query.andQualifier(exp2);
    Expression exp3 = ExpressionFactory.matchDbExp(
      TurbineUser.USER_ID_PK_COLUMN, uid);
    query.andQualifier(exp3);

    // 終了日時
    Expression exp11 = ExpressionFactory.greaterOrEqualExp(
      EipTTodo.END_DATE_PROPERTY, viewStart.getValue());
    // 開始日時
    Expression exp12 = ExpressionFactory.lessOrEqualExp(
      EipTTodo.START_DATE_PROPERTY, viewEndCrt.getValue());

    // 開始日時のみ指定されている ToDo を検索
    Expression exp21 = ExpressionFactory.lessOrEqualExp(
      EipTTodo.START_DATE_PROPERTY, viewEndCrt.getValue());
    Expression exp22 = ExpressionFactory.matchExp(EipTTodo.END_DATE_PROPERTY,
      ToDoUtils.getEmptyDate());

    // 終了日時のみ指定されている ToDo を検索
    Expression exp31 = ExpressionFactory.greaterOrEqualExp(
      EipTTodo.END_DATE_PROPERTY, viewStart.getValue());
    Expression exp32 = ExpressionFactory.matchExp(EipTTodo.START_DATE_PROPERTY,
      ToDoUtils.getEmptyDate());

    query.andQualifier((exp11.andExp(exp12)).orExp(exp21.andExp(exp22)).orExp(
      exp31.andExp(exp32)));
    return query;
  }

  /**
   * 表示開始日時を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewStart() {
    return viewStart;
  }

  /**
   * 表示終了日時を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewEnd() {
    return viewEnd;
  }

  /**
   * 表示タイプを取得します。
   * 
   * @return
   */
  public String getViewtype() {
    return viewtype;
  }

  /**
   * 表示終了日時 (Criteria) を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewEndCrt() {
    return viewEndCrt;
  }

  /**
   * 前の日を取得します。
   * 
   * @return
   */
  public ALDateTimeField getPrevDate() {
    return prevDate;
  }

  /**
   * 前の週を取得します。
   * 
   * @return
   */
  public ALDateTimeField getPrevWeek() {
    return prevWeek;
  }

  /**
   * 次の日を取得します。
   * 
   * @return
   */
  public ALDateTimeField getNextDate() {
    return nextDate;
  }

  /**
   * 次の週を取得します。
   * 
   * @return
   */
  public ALDateTimeField getNextWeek() {
    return nextWeek;
  }

  /**
   * 今日を取得します。
   * 
   * @return
   */
  public ALDateTimeField getToday() {
    return today;
  }

  /**
   * 先月を取得する．
   * 
   * @return
   */
  public ALDateTimeField getPrevMonth() {
    return prevMonth;
  }

  /**
   * 来月を取得する．
   * 
   * @return
   */
  public ALDateTimeField getNextMonth() {
    return nextMonth;
  }

  /**
   * 期間スケジュールコンテナを取得します。
   * 
   * @return
   */
  public List<ScheduleTermWeekContainer> getTermContainer() {
    return termWeekConList;
  }

  /**
   * 週間スケジュールコンテナを取得します。
   * 
   * @return
   */
  public ScheduleWeekContainer getContainer() {
    return weekCon;
  }

  public List<ScheduleToDoWeekContainer> getWeekToDoContainerList() {
    return weekTodoConList;
  }

  public void setPortletId(String id) {
    portletId = id;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   * 
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
  }

  public boolean hasAuthoritySelfInsert() {
    return hasAuthoritySelfInsert;
  }

}
