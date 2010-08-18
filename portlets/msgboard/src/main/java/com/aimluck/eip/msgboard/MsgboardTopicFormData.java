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
package com.aimluck.eip.msgboard;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategoryMap;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardFile;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardTopic;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.fileupload.beans.FileuploadLiteBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.msgboard.util.MsgboardUtils;
import com.aimluck.eip.orm.DatabaseOrmService;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.whatsnew.util.WhatsNewUtils;

/**
 * 掲示板トピックのフォームデータを管理するクラスです。 <BR>
 * 
 */
public class MsgboardTopicFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MsgboardTopicFormData.class.getName());

  /** トピック名 */
  private ALStringField topic_name;

  /** カテゴリ ID */
  private ALNumberField category_id;

  /** カテゴリ名 */
  private ALStringField category_name;

  /** メモ */
  private ALStringField note;

  /** カテゴリ一覧 */
  private List<MsgboardCategoryResultData> categoryList;

  /** */
  private boolean is_new_category;

  /** 添付ファイルリスト */
  private List<FileuploadLiteBean> fileuploadList = null;

  /** 添付フォルダ名 */
  private String folderName = null;

  private int uid;

  private EipTMsgboardCategory category;

  private DataContext dataContext;

  private String org_id;

  private String aclPortletFeature = null;

  /** 閲覧権限の有無 */
  @SuppressWarnings("unused")
  private boolean hasAclCategoryList;

  /** 他ユーザーの作成したトピックの編集権限 */
  private boolean hasAclUpdateTopicOthers;

  /** 他ユーザーの作成したトピックの削除権限 */
  private boolean hasAclDeleteTopicOthers;

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   * @see com.aimluck.eip.common.ALAbstractFormData#init(com.aimluck.eip.modules.actions.common.ALAction,
   *      org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);
    is_new_category = rundata.getParameters().getBoolean("is_new_category");

    uid = ALEipUtils.getUserId(rundata);
    org_id = DatabaseOrmService.getInstance().getOrgId(rundata);

    dataContext = DatabaseOrmService.getInstance().getDataContext();

    folderName = rundata.getParameters().getString("folderName");

    ALAccessControlFactoryService aclservice = (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
      .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAclCategoryList = aclhandler.hasAuthority(ALEipUtils.getUserId(rundata),
      ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_CATEGORY,
      ALAccessControlConstants.VALUE_ACL_LIST);

    hasAclDeleteTopicOthers = aclhandler.hasAuthority(ALEipUtils
      .getUserId(rundata),
      ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC_OTHER,
      ALAccessControlConstants.VALUE_ACL_DELETE);

    hasAclUpdateTopicOthers = aclhandler.hasAuthority(ALEipUtils
      .getUserId(rundata),
      ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC_OTHER,
      ALAccessControlConstants.VALUE_ACL_UPDATE);
  }

  /**
   * 各フィールドを初期化します。 <BR>
   * 
   * @see com.aimluck.eip.common.ALData#initField()
   */
  public void initField() {
    // トピック名
    topic_name = new ALStringField();
    topic_name.setFieldName("タイトル");
    topic_name.setTrim(true);
    // カテゴリID
    category_id = new ALNumberField();
    category_id.setFieldName("カテゴリ");
    // カテゴリ
    category_name = new ALStringField();
    category_name.setFieldName("カテゴリ名");
    // メモ
    note = new ALStringField();
    note.setFieldName("内容");
    note.setTrim(false);

    fileuploadList = new ArrayList<FileuploadLiteBean>();

    aclPortletFeature = ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC;
  }

  /**
   * 
   * @param rundata
   * @param context
   */
  public void loadCategoryList(RunData rundata, Context context) {
    categoryList = MsgboardUtils.loadCategoryList(rundata);
  }

  /**
   * 掲示板の各フィールドに対する制約条件を設定します。 <BR>
   * 
   * @see com.aimluck.eip.common.ALAbstractFormData#setValidator()
   */
  @Override
  protected void setValidator() {
    // トピック名必須項目
    topic_name.setNotNull(true);
    // トピック名の文字数制限
    topic_name.limitMaxLength(50);
    // メモ必須項目
    note.setNotNull(true);
    // メモの文字数制限
    note.limitMaxLength(10000);
    if (is_new_category) {
      // カテゴリ名必須項目
      category_name.setNotNull(true);
      // カテゴリ名文字数制限
      category_name.limitMaxLength(50);
    }
  }

  /**
   * トピックのフォームに入力されたデータの妥当性検証を行います。 <BR>
   * 
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   * @see com.aimluck.eip.common.ALAbstractFormData#validate(java.util.ArrayList)
   */
  @Override
  protected boolean validate(List<String> msgList) {
    // トピック名
    topic_name.validate(msgList);
    // メモ
    note.validate(msgList);
    if (is_new_category) {
      // カテゴリ名
      category_name.validate(msgList);
    }
    return (msgList.size() == 0);

  }

  /**
   * トピックをデータベースから読み出します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   * @see com.aimluck.eip.common.ALAbstractFormData#loadFormData(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      // オブジェクトモデルを取得
      EipTMsgboardTopic topic = MsgboardUtils.getEipTMsgboardParentTopic(
        rundata, context, false);
      if (topic == null) {
        return false;
      }

      // トピック名
      topic_name.setValue(topic.getTopicName());
      // カテゴリID
      category_id.setValue(topic.getEipTMsgboardCategory().getCategoryId());
      // 内容
      note.setValue(topic.getNote());
      // ファイル
      SelectQuery query = new SelectQuery(EipTMsgboardFile.class);
      query.andQualifier(ExpressionFactory.matchDbExp(
        EipTMsgboardFile.EIP_TMSGBOARD_TOPIC_PROPERTY, topic.getTopicId()));
      fileuploadList = query.perform();

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * トピックをデータベースから削除します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   * @see com.aimluck.eip.common.ALAbstractFormData#deleteFormData(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {

      // FIX_ME イベントログのために一度IDと名前を取得
      int parentid = Integer.parseInt(ALEipUtils.getTemp(rundata, context,
        ALEipConstants.ENTITY_ID));

      EipTMsgboardTopic parent = (EipTMsgboardTopic) DataObjectUtils
        .objectForPK(dataContext, EipTMsgboardTopic.class, (long) parentid);

      // オブジェクトモデルを取得
      List<EipTMsgboardTopic> list;

      if (this.hasAclDeleteTopicOthers) {
        list = MsgboardUtils.getEipTMsgboardTopicListToDeleteTopic(rundata,
          context, true);
      } else {
        list = MsgboardUtils.getEipTMsgboardTopicListToDeleteTopic(rundata,
          context, false);
      }

      if (list == null) {
        // 指定した トピック ID のレコードが見つからない場合
        logger.debug("[MsgboardTopicFormData] Not found List...");
        throw new ALPageNotFoundException();
      }

      List<Integer> topicIdList = new ArrayList<Integer>();
      EipTMsgboardTopic topic;
      int size = list.size();
      for (int i = 0; i < size; i++) {
        topic = list.get(i);
        topicIdList.add(topic.getTopicId());
      }

      // トピックを削除
      SelectQuery<EipTMsgboardTopic> query = new SelectQuery<EipTMsgboardTopic>(
        EipTMsgboardTopic.class);
      Expression exp = ExpressionFactory.inDbExp(
        EipTMsgboardTopic.TOPIC_ID_PK_COLUMN, topicIdList);
      query.setQualifier(exp);

      List<EipTMsgboardTopic> topics = query.perform();

      List<String> fpaths = new ArrayList<String>();
      if (topics.size() > 0) {
        int tsize = topics.size();
        for (int i = 0; i < tsize; i++) {
          List<?> files = topics.get(i).getEipTMsgboardFileArray();
          if (files != null && files.size() > 0) {
            int fsize = files.size();
            for (int j = 0; j < fsize; j++) {
              fpaths.add(((EipTMsgboardFile) files.get(j)).getFilePath());
            }
          }
        }
      }

      dataContext.deleteObjects(topics);
      dataContext.commitChanges();

      if (fpaths.size() > 0) {
        // ローカルファイルに保存されているファイルを削除する．
        File file = null;
        int fsize = fpaths.size();
        for (int i = 0; i < fsize; i++) {
          file = new File(MsgboardUtils.getSaveDirPath(org_id, uid)
            + fpaths.get(i));
          if (file.exists()) {
            file.delete();
          }
        }
      }

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        parent.getTopicId(), ALEventlogConstants.PORTLET_TYPE_MSGBOARD_TOPIC,
        parent.getTopicName());

    } catch (Exception e) {
      // TODO: エラー処理
      logger.error("[MsgboardCategorySelectData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   * トピックをデータベースに格納します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   * @see com.aimluck.eip.common.ALAbstractFormData#insertFormData(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      if (is_new_category) {
        // カテゴリの登録処理
        if (!insertCategoryData(rundata, context)) {
          // TODO ロールバック
        }
      } else {
        category = (EipTMsgboardCategory) DataObjectUtils.objectForPK(
          dataContext, EipTMsgboardCategory.class, Integer
            .valueOf((int) category_id.getValue()));
      }

      // 新規オブジェクトモデル
      EipTMsgboardTopic topic = (EipTMsgboardTopic) dataContext
        .createAndRegisterNewObject(EipTMsgboardTopic.class);
      // トピック名
      topic.setTopicName(topic_name.getValue());
      // 親トピックID
      topic.setParentId(Integer.valueOf(0));
      // カテゴリID
      topic.setEipTMsgboardCategory(category);
      // ユーザーID
      topic.setOwnerId(Integer.valueOf(uid));
      // メモ
      topic.setNote(note.getValue());
      // 作成者
      topic.setCreateUserId(Integer.valueOf(uid));
      // 更新者
      topic.setUpdateUserId(Integer.valueOf(uid));
      // 作成日
      topic.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      topic.setUpdateDate(Calendar.getInstance().getTime());

      // ファイルをデータベースに登録する．
      if (!MsgboardUtils.insertFileDataDelegate(rundata, context, topic,
        fileuploadList, folderName, msgList)) {
        return false;
      }

      dataContext.commitChanges();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        topic.getTopicId(), ALEventlogConstants.PORTLET_TYPE_MSGBOARD_TOPIC,
        topic.getTopicName());

      /* 自分以外の全員に新着ポートレット登録 */
      if ("T".equals(topic.getEipTMsgboardCategory().getPublicFlag())) {
        WhatsNewUtils.insertWhatsNewPublic(
          WhatsNewUtils.WHATS_NEW_TYPE_MSGBOARD_TOPIC, topic.getTopicId(), uid);
      } else {
        List<Integer> userIds = MsgboardUtils.getWhatsNewInsertList(rundata,
          category.getCategoryId().intValue(), category.getPublicFlag());

        int u_size = userIds.size();
        for (int i = 0; i < u_size; i++) {
          Integer _id = userIds.get(i);
          WhatsNewUtils.insertWhatsNew(
            WhatsNewUtils.WHATS_NEW_TYPE_MSGBOARD_TOPIC, topic.getTopicId()
              .intValue(), _id.intValue());
        }
      }

      File folder = FileuploadUtils.getFolder(org_id, uid, folderName);
      // 添付ファイル保存先のフォルダを削除
      FileuploadUtils.deleteFolder(folder);
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * トピックカテゴリをデータベースに格納します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @see com.aimluck.eip.common.ALAbstractFormData#insertFormData(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context, java.util.ArrayList)
   */
  private boolean insertCategoryData(RunData rundata, Context context) {
    try {

      setAclPortletFeature(ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_CATEGORY);
      doCheckAclPermission(rundata, context,
        ALAccessControlConstants.VALUE_ACL_INSERT);
      setAclPortletFeature(ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC);

      int userid = ALEipUtils.getUserId(rundata);

      TurbineUser tuser = (TurbineUser) DataObjectUtils.objectForPK(
        dataContext, TurbineUser.class, Integer.valueOf(userid));

      // 新規オブジェクトモデル
      category = (EipTMsgboardCategory) dataContext
        .createAndRegisterNewObject(EipTMsgboardCategory.class);
      // カテゴリ名
      category.setCategoryName(category_name.getValue());
      // 公開区分
      category.setPublicFlag(MsgboardUtils.PUBLIC_FLG_VALUE_PUBLIC);
      // カテゴリメモ
      category.setNote("");
      // ユーザーID
      category.setTurbineUser(tuser);
      // 作成日
      category.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      category.setUpdateDate(Calendar.getInstance().getTime());

      // マップの登録
      EipTMsgboardCategoryMap map = (EipTMsgboardCategoryMap) dataContext
        .createAndRegisterNewObject(EipTMsgboardCategoryMap.class);
      map.setEipTMsgboardCategory(category);
      map.setUserId(Integer.valueOf(userid));
      map.setStatus(MsgboardUtils.STAT_VALUE_OWNER);

      dataContext.commitChanges();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        category.getCategoryId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_CATEGORY,
        category.getCategoryName());

    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * データベースに格納されているトピックを更新します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return TRUE 成功 FALSE 失敗
   * @see com.aimluck.eip.common.ALAbstractFormData#updateFormData(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      if (is_new_category) {
        // カテゴリの登録処理
        if (!insertCategoryData(rundata, context)) {

        }
      } else {
        category = (EipTMsgboardCategory) DataObjectUtils.objectForPK(
          dataContext, EipTMsgboardCategory.class, Integer
            .valueOf((int) category_id.getValue()));
      }

      // 新規オブジェクトモデル
      EipTMsgboardTopic topic = MsgboardUtils.getEipTMsgboardParentTopic(
        rundata, context, false);
      // トピック名
      topic.setTopicName(topic_name.getValue());
      // カテゴリID
      topic.setEipTMsgboardCategory(category);
      // メモ
      topic.setNote(note.getValue());
      // 更新者
      topic.setUpdateUserId(Integer.valueOf(uid));
      // 更新日
      topic.setUpdateDate(Calendar.getInstance().getTime());

      // ファイルをデータベースに登録する．
      if (!MsgboardUtils.insertFileDataDelegate(rundata, context, topic,
        fileuploadList, folderName, msgList)) {
        return false;
      }

      dataContext.commitChanges();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        topic.getTopicId(), ALEventlogConstants.PORTLET_TYPE_MSGBOARD_TOPIC,
        topic.getTopicName());

      /* 自分以外の全員に新着ポートレット登録 */
      if ("T".equals(topic.getEipTMsgboardCategory().getPublicFlag())) {
        WhatsNewUtils.insertWhatsNewPublic(
          WhatsNewUtils.WHATS_NEW_TYPE_MSGBOARD_TOPIC, topic.getTopicId(), uid);
      } else {
        List<Integer> userIds = MsgboardUtils.getWhatsNewInsertList(rundata,
          category.getCategoryId().intValue(), category.getPublicFlag());

        int u_size = userIds.size();
        for (int i = 0; i < u_size; i++) {
          Integer _id = userIds.get(i);
          WhatsNewUtils.insertWhatsNew(
            WhatsNewUtils.WHATS_NEW_TYPE_MSGBOARD_TOPIC, topic.getTopicId()
              .intValue(), _id.intValue());
        }
      }

      File folder = FileuploadUtils.getFolder(org_id, uid, folderName);
      // 添付ファイル保存先のフォルダを削除
      FileuploadUtils.deleteFolder(folder);
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return false;
    }
    return true;
  }

  /**
   * 
   * @see com.aimluck.eip.common.ALAbstractFormData#setFormData(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    boolean res = super.setFormData(rundata, context, msgList);

    try {
      fileuploadList = FileuploadUtils.getFileuploadList(rundata);
    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    return res;
  }

  /**
   * カテゴリIDを取得します。 <BR>
   * 
   * @return
   */
  public ALNumberField getCategoryId() {
    return category_id;
  }

  /**
   * メモを取得します。 <BR>
   * 
   * @return
   */
  public ALStringField getNote() {
    return note;
  }

  /**
   * トピック名を取得します。 <BR>
   * 
   * @return
   */
  public ALStringField getTopicName() {
    return topic_name;
  }

  /**
   * カテゴリ一覧を取得します。 <BR>
   * 
   * @return
   */
  public List<MsgboardCategoryResultData> getCategoryList() {
    return categoryList;
  }

  /**
   * @return
   */
  public boolean isNewCategory() {
    return is_new_category;
  }

  /**
   * @return
   */
  public void setIsNewCategory(boolean bool) {
    is_new_category = bool;
  }

  /**
   * カテゴリ名を取得します。
   * 
   * @return
   */
  public ALStringField getCategoryName() {
    return category_name;
  }

  public List<FileuploadLiteBean> getAttachmentFileNameList() {
    return fileuploadList;
  }

  public String getFolderName() {
    return folderName;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   * 
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return aclPortletFeature;
  }

  public void setAclPortletFeature(String aclPortletFeature) {
    this.aclPortletFeature = aclPortletFeature;
  }

  /**
   * 他ユーザのトピックを編集する権限があるかどうかを返します。
   * 
   * @return
   */
  public boolean hasAclUpdateTopicOthers() {
    return hasAclUpdateTopicOthers;
  }

  /**
   * 他ユーザのトピックを削除する権限があるかどうかを返します。
   * 
   * @return
   */
  public boolean hasAclDeleteTopicOthers() {
    return hasAclDeleteTopicOthers;
  }
}
