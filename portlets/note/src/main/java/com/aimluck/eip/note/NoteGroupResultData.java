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
package com.aimluck.eip.note;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.common.ALData;

/**
 * 伝言メモの送信先に指定できるグループのResultDataです。 <br />
 */
public class NoteGroupResultData implements ALData {

  /** ユーザ ID */
  private ALNumberField user_id = null;

  /** ユーザ名 */
  private ALStringField user_name = null;

  /** スケジュール */
  private ALStringField schedule = null;

  /** 受信未読数 */
  private ALNumberField unreadReceivedNoteCount = null;

  /** 送信未読数 */
  // private ALNumberField unreadSentNoteCount = null;
  /** 新着／未読／既読フラグ */
  private ALStringField new_note_stat = null;

  /** 新着／未読／既読の画像へのファイルパス */
  private String new_note_image_path = null;

  /** 新着／未読／既読の画像に対する説明文 */
  private String new_note_image_description = null;

  /**
   * @see com.aimluck.eip.common.ALData#initField()
   */
  public void initField() {
    user_id = new ALNumberField();
    user_name = new ALStringField();
    schedule = new ALStringField();
    unreadReceivedNoteCount = new ALNumberField();
    // unreadSentNoteCount = new ALNumberField();
    new_note_stat = new ALStringField();
  }

  /**
   * @return
   */
  public ALStringField getSchedule() {
    return schedule;
  }

  /**
   * @return
   */
  public ALNumberField getUserId() {
    return user_id;
  }

  /**
   * @return
   */
  public ALStringField getUserName() {
    return user_name;
  }

  /**
   * @param field
   */
  public void setSchedule(String field) {
    schedule.setValue(field);
  }

  /**
   * @param field
   */
  public void setUserId(int field) {
    user_id.setValue(field);
  }

  /**
   * @param field
   */
  public void setUserName(String field) {
    user_name.setValue(field);
  }

  public ALNumberField getUnreadReceivedNoteCount() {
    return unreadReceivedNoteCount;
  }

  public void setUnreadReceivedNoteCount(int field) {
    unreadReceivedNoteCount.setValue(field);
  }

  // public ALNumberField getUnreadSentNoteCount(){
  // return unreadSentNoteCount;
  // }
  //
  // public void setUnreadSentNoteCount(int field){
  // unreadSentNoteCount.setValue(field);
  // }

  public ALStringField getNewNoteStat() {
    return new_note_stat;
  }

  public void setNewNoteStat(String value) {
    new_note_stat.setValue(value);
  }

  /**
   * 新着／未読／既読の画像ファイルへのパスを返す．
   * 
   * @return
   */
  public void setNewNoteImage(String newNoteImagePath) {
    this.new_note_image_path = newNoteImagePath;
  }

  /**
   * 新着／未読／既読の画像ファイルへのパスを返す．
   * 
   * @return
   */
  public String getNewNoteImage() {
    return new_note_image_path;
  }

  public void setNewNoteImageDescription(String newNoteImageDescription) {
    this.new_note_image_description = newNoteImageDescription;
  }

  public String getNewNoteImageDescription() {
    return new_note_image_description;
  }
}
