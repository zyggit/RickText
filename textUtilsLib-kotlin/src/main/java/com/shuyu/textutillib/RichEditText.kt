package com.shuyu.textutillib

import android.content.Context
import android.text.Editable
import android.text.Html
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.MotionEvent

import com.shuyu.textutillib.listener.OnEditTextUtilJumpListener
import com.shuyu.textutillib.model.TopicModel
import com.shuyu.textutillib.model.UserModel

import java.util.ArrayList
import java.util.HashMap
import java.util.regex.Pattern

/**
 * 富文本设置 话题、at某人、表情
 * Created by guoshuyu on 2017/8/18.
 */

class RichEditText : MentionEditText {

    /**
     * 默认最长输入
     */
    /**
     * 最长输入
     *
     * @param richMaxLength
     */
    var richMaxLength = 9999

    /**
     * 表情大小
     */
    /**
     * 表情大小
     *
     * @param richIconSize
     */
    var richIconSize: Int = 0

    /**
     * 是否可以在列表增加触摸滑动
     */
    /**
     * 是否可以点击滑动
     *
     * @param isRequest
     */
    var isRequestTouchIn = false

    /**
     * 用户at
     */
    private var nameList: MutableList<UserModel>? = null

    /**
     * 话题
     */
    private var topicList: MutableList<TopicModel>? = null

    /**
     * 输入监控回调
     */
    private var editTextAtUtilJumpListener: OnEditTextUtilJumpListener? = null
    /**
     * At颜色
     */
    private var colorTopic = "#0000FF"
    /**
     * 话题颜色
     */
    private var colorAtUser = "#f77521"

    private var deleteByEnter: Boolean = false

    /**
     * 返回真实无添加的数据
     *
     * @return
     */
    val realUserList: List<UserModel>
        get() {
            val list = ArrayList<UserModel>()
            if (nameList == null) {
                return list
            }
            for (userModel in nameList!!) {
                list.add(UserModel(userModel.user_name!!.replace("@", "").replace("\b", ""), userModel.user_id))

            }
            return list
        }

    /**
     * 返回真实无添加的数据
     *
     * @return
     */
    val realTopicList: List<TopicModel>
        get() {
            val list = ArrayList<TopicModel>()
            if (topicList == null) {
                return list
            }
            for (topicModel in topicList!!) {
                list.add(TopicModel(topicModel.topicName!!.replace("#", "").replace("#", ""), topicModel.topicId))
            }
            return list

        }

    /**
     * 提交真实文本可以替换了\b
     *
     * @return
     */
    val realText: String
        get() {
            if (TextUtils.isEmpty(text)) {
                return ""
            }
            val text = text.toString()
            return text.replace("\\u0008".toRegex(), " ")
        }


    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }


    private fun init(context: Context, attrs: AttributeSet?) {

        if (isInEditMode)
            return

        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.RichEditText)
            val textLength = array.getInteger(R.styleable.RichEditText_richMaxLength, 9999)
            val iconSize = array.getDimension(R.styleable.RichEditText_richIconSize, 0f).toInt().toFloat()
            val colorAtUser = array.getString(R.styleable.RichEditText_richEditColorAtUser)
            val colorTopic = array.getString(R.styleable.RichEditText_richEditColorTopic)
            richMaxLength = textLength
            val filters = arrayOf<InputFilter>(InputFilter.LengthFilter(richMaxLength))
            setFilters(filters)
            if (iconSize == 0f) {
                richIconSize = dip2px(context, 20f)
            }
            if (!TextUtils.isEmpty(colorAtUser)) {
                this.colorAtUser = colorAtUser
            }
            if (!TextUtils.isEmpty(colorTopic)) {
                this.colorTopic = colorTopic
            }
            array.recycle()
        }

        resolveAtPersonEditText()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(isRequestTouchIn)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_UP -> parent.requestDisallowInterceptTouchEvent(false)
        }
        return super.onTouchEvent(event)
    }


    /**
     * dip转为PX
     */
    private fun dip2px(context: Context, dipValue: Float): Int {
        val fontScale = context.resources.displayMetrics.density
        return (dipValue * fontScale + 0.5f).toInt()
    }


    /**
     * 删除@某人里面的缓存列表
     */
    private fun resolveDeleteName() {
        val selectionStart = selectionStart
        var lastPos = 0
        for (i in nameList!!.indices) { //循环遍历整个输入框的所有字符
            lastPos = text.toString().indexOf(nameList!![i].user_name!!.replace("\b", ""), lastPos);
            if (lastPos != -1) {
                if (selectionStart > lastPos && selectionStart <= lastPos + nameList!![i].user_name!!.length) {
                    nameList!!.removeAt(i)
                    return
                } else {
                    lastPos++
                }
            } else {
                lastPos += nameList!![i].user_name!!.length
            }
        }
    }

    /**
     * 删除@某人里面的缓存列表
     */
    private fun resolveDeleteTopic() {
        if (topicList == null) {
            return
        }
        val selectionStart = selectionStart
        var lastPos = 0
        for (i in topicList!!.indices) { //循环遍历整个输入框的所有字符
            lastPos = text.toString().indexOf(topicList!![i].topicName!!, lastPos)
            if (lastPos != -1) {
                if (selectionStart > lastPos && selectionStart <= lastPos + topicList!![i].topicName!!.length) {
                    topicList!!.removeAt(i)
                    return
                } else {
                    lastPos++
                }
            } else {
                lastPos += topicList!![i].topicName!!.length
            }
        }
    }

    private fun resolveDeleteList(text: String) {
        if (topicList != null && topicList!!.size > 0) {
            var lastMentionIndex = -1
            val matcher = mTopicPattern?.matcher(text)
            if (matcher != null) {
                while (matcher.find()) {
                    val mentionText = matcher.group()
                    val start: Int
                    if (lastMentionIndex != -1) {
                        start = getText().toString().indexOf(mentionText, lastMentionIndex)
                    } else {
                        start = getText().toString().indexOf(mentionText)
                    }
                    val end = start + mentionText.length
                    lastMentionIndex = end
                    for (i in topicList!!.indices) {
                        val topicModel = topicList!![i]
                        if (topicModel.topicName == mentionText && getRangeOfClosestMentionString(start, end) != null) {
                            topicList!!.remove(topicModel)
                            break
                        }
                    }
                }
            }
        }

        if (nameList != null && nameList!!.size > 0) {
            var lastMentionIndex = -1
            val matcher = mPattern?.matcher(text)
            if (matcher != null) {
                while (matcher.find()) {
                    var mentionText = matcher.group()
                    val start: Int
                    if (lastMentionIndex != -1) {
                        start = getText().toString().indexOf(mentionText, lastMentionIndex)
                    } else {
                        start = getText().toString().indexOf(mentionText)
                    }
                    val end = start + mentionText.length
                    mentionText = mentionText.substring(mentionText.lastIndexOf("@"), mentionText.length)
                    lastMentionIndex = end
                    for (i in nameList!!.indices) {
                        val userModel = nameList!![i]
                        if (userModel.user_name!!.replace("\b", "") == mentionText.replace("\b", "") && getRangeOfClosestMentionString(start, end) != null) {
                            nameList!!.remove(userModel)
                            break
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理光标不插入在AT某人字段上
     */
    private fun resolveEditTextClick() {
        if (TextUtils.isEmpty(text))
            return
        val selectionStart = selectionStart
        if (selectionStart > 0) {
            var lastPos = 0
            var success = false
            for (i in nameList!!.indices) {
                lastPos = text.toString().indexOf(
                        nameList!![i].user_name!!, lastPos)
                if (lastPos != -1) {
                    if (selectionStart >= lastPos && selectionStart <= lastPos + nameList!![i].user_name!!.length) {
                        setSelection(lastPos + nameList!![i].user_name!!.length)
                        success = true
                    }
                    lastPos += nameList!![i].user_name!!.length
                }
            }

            if (!success && topicList != null) {
                lastPos = 0
                for (i in topicList!!.indices) {
                    lastPos = text.toString().indexOf(
                            topicList!![i].topicName!!, lastPos)
                    if (lastPos != -1) {
                        if (selectionStart >= lastPos && selectionStart <= lastPos + topicList!![i].topicName!!.length) {
                            setSelection(lastPos + topicList!![i].topicName!!.length)
                        }
                        lastPos += topicList!![i].topicName!!.length
                    }
                }
            }
        }
    }

    /**
     * 监听字符变化与点击事件
     */
    private fun resolveAtPersonEditText() {
        addTextChangedListener(object : TextWatcher {

            private var length = 0
            private var delIndex = -1
            private var beforeCount: Int = 0

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                beforeCount = s.toString().length
                if (count == 1) {
                    val deleteSb = s.toString().substring(start, start + 1)
                    if ("\b" == deleteSb) {
                        delIndex = s.toString().lastIndexOf("@", start)
                        length = start - delIndex
                    } else if ("#" == deleteSb && !deleteByEnter) {
                        delIndex = s.toString().lastIndexOf("#", start - 1)
                        length = start - delIndex
                    }
                    deleteByEnter = false
                } else if (after < count && count - after > 1) {
                    // 大批量删除处理的列表处理
                    resolveDeleteList(s.toString().substring(start, start + count))
                }
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val setMsg = s.toString()
                if (delIndex != -1) {
                    if (length > 1) {
                        resolveDeleteName()
                        resolveDeleteTopic()
                        val position = delIndex
                        text.replace(position, position + length, "")
                        setSelection(position)
                    }
                    delIndex = -1
                } else {
                    if (setMsg.length >= beforeCount && selectionEnd > 0 && setMsg[selectionEnd - 1] == '@') {
                        if (editTextAtUtilJumpListener != null) {
                            editTextAtUtilJumpListener!!.notifyAt()
                        }
                    } else if (setMsg.length >= beforeCount && selectionEnd > 0 && setMsg[selectionEnd - 1] == '#') {
                        if (editTextAtUtilJumpListener != null) {
                            editTextAtUtilJumpListener!!.notifyTopic()
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {

            }
        })

        setOnClickListener { resolveEditTextClick() }

    }

    /**
     * 处理话题和表情
     *
     * @param context   上下文
     * @param text      输入文本
     * @param color     颜色
     * @param listTopic 话题列表
     * @return Spannable
     */
    private fun resolveTopicInsert(context: Context, text: String, color: String, listTopic: List<TopicModel>?): Spannable {
        val spannable: Spannable
        if (listTopic != null && listTopic.size > 0) {
            val topics = HashMap<String, String>()
            for (topicModel in listTopic) {
                topics.put(topicModel.topicName, topicModel.topicName)
            }
            //查找##
            val length = text.length
            val pattern = Pattern.compile("#[^\\s]+?#")
            val matcher = pattern.matcher(text)
            val spannableStringBuilder = SpannableStringBuilder(text)
            for (i in 0 until length) {
                if (matcher.find()) {
                    val name = text.substring(matcher.start(), matcher.end())
                    if (topics.containsKey(name)) {
                        //直接用span会导致后面没文字的时候新输入的一起变色
                        val htmlText = Html.fromHtml(String.format("<font color='%s'>$name</font>", color))
                        spannableStringBuilder.replace(matcher.start(), matcher.start() + name.length, htmlText)
                    }
                }
            }

            spannable = spannableStringBuilder
            SmileUtils.addSmiles(context, spannable)
        } else {
            spannable = TextCommonUtils.getEmojiText(context, text)

        }
        SmileUtils.addSmiles(context, spannable)
        return spannable
    }

    /**
     * 处理at某人
     *
     * @param text      输入文本
     * @param spannable 处理过的文本
     * @param color     颜色
     * @param listUser  用户列表
     * @return Spannable
     */
    private fun resolveAtInsert(text: String, spannable: Spannable, color: String, listUser: List<UserModel>?): Spannable {

        if (listUser == null || listUser.size <= 0) {
            return spannable
        }

        //此处保存名字的键值
        val names = HashMap<String, String>()
        if (listUser.size > 0) {
            for (userModel in listUser) {
                names.put(userModel.user_name, userModel.user_name)
            }
        }
        val length = spannable.length
        val pattern = Pattern.compile("@[^\\s]+\\s?")
        val matcher = pattern.matcher(spannable)
        val spannableStringBuilder = SpannableStringBuilder(spannable)
        for (i in 0 until length) {
            if (matcher.find()) {
                val name = text.substring(matcher.start(), matcher.end())
                if (names.containsKey(name.replace("\b", "").replace(" ", ""))) {
                    //直接用span会导致后面没文字的时候新输入的一起变色
                    val htmlText = Html.fromHtml(String.format("<font color='%s'>$name</font>", color))
                    spannableStringBuilder.replace(matcher.start(), matcher.start() + name.length, htmlText)
                    val index = matcher.start() + htmlText.length
                    if (index < text.length) {
                        if (" " == text.subSequence(index - 1, index)) {
                            spannableStringBuilder.replace(index - 1, index, "\b")
                        }
                    } else {
                        if (text.substring(index - 1) == " ") {
                            spannableStringBuilder.replace(index - 1, index, "\b")
                        } else {
                            //如果是最后面的没有空格，补上\b
                            spannableStringBuilder.insert(index, "\b")
                        }
                    }
                }
            }
        }
        return spannableStringBuilder
    }

    /********************公开接口 */

    /**
     * 设置数据列表
     *
     * @param nameList  at用户
     * @param topicList 话题
     */
    fun setModelList(nameList: MutableList<UserModel>, topicList: MutableList<TopicModel>) {
        this.nameList = nameList
        this.topicList = topicList
    }


    fun setRichEditTopicList(list: MutableList<TopicModel>?) {
        if (list != null) {
            this.topicList = list
        }
    }

    fun setRichEditNameList(list: MutableList<UserModel>?) {
        if (list != null) {
            this.nameList = list
        }
    }

    fun setRichEditColorAtUser(color: String) {
        this.colorAtUser = color
    }

    fun setRichEditColorTopic(color: String) {
        this.colorTopic = color
    }

    /**
     * 话题颜色
     *
     * @param colorTopic 类似#f77500的颜色格式
     */
    fun setColorTopic(colorTopic: String) {
        this.colorTopic = colorTopic
    }

    /**
     * at人颜色
     *
     * @param colorAtUser 类似#f77500的颜色格式
     */
    fun setColorAtUser(colorAtUser: String) {
        this.colorAtUser = colorAtUser
    }

    /**
     * 添加了@的加入
     *
     * @param userModel 用户实体
     */
    fun resolveText(userModel: UserModel) {
        val userName = userModel.user_name
        userModel.user_name = userName!! + "\b"
        nameList!!.add(userModel)

        val index = selectionStart
        val spannableStringBuilder = SpannableStringBuilder(text)
        //直接用span会导致后面没文字的时候新输入的一起变色
        val htmlText = Html.fromHtml(String.format("<font color='%s'>$userName</font>", colorAtUser))
        spannableStringBuilder.insert(index, htmlText)
        spannableStringBuilder.insert(index + htmlText.length, "\b")
        setText(spannableStringBuilder)
        setSelection(index + htmlText.length + 1)
    }

    /**
     * 插入了话题
     *
     * @param topicModel 话题实体
     */
    fun resolveTopicText(topicModel: TopicModel) {
        topicList!!.add(topicModel)
        val index = selectionStart
        val spannableStringBuilder = SpannableStringBuilder(text)
        //直接用span会导致后面没文字的时候新输入的一起变色
        val htmlText = Html.fromHtml(String.format("<font color='%s'>" + topicModel.topicName + "</font>", colorTopic))
        spannableStringBuilder.insert(index, htmlText)
        setText(spannableStringBuilder)
        setSelection(index + htmlText.length)
    }


    /**
     * 编辑框输入了@后的跳转
     *
     * @param editTextAtUtilJumpListener 跳转回调
     */
    fun setEditTextAtUtilJumpListener(editTextAtUtilJumpListener: OnEditTextUtilJumpListener) {
        this.editTextAtUtilJumpListener = editTextAtUtilJumpListener
    }

    /**
     * 初始户处理插入的文本
     *
     * @param context  上下文
     * @param text     需要处理的文本
     * @param listUser 需要处理的at某人列表
     */
    fun resolveInsertText(context: Context, text: String, listUser: List<UserModel>, listTopic: List<TopicModel>) {

        if (TextUtils.isEmpty(text))
            return

        //设置表情和话题
        val spannable = resolveTopicInsert(context, text, colorTopic, listTopic)
        setText(spannable)

        //设置@
        val span = resolveAtInsert(text, spannable, colorAtUser, listUser)
        setText(span)

        setSelection(getText().length)
    }


    /**
     * 按了话题按键的数据返回处理
     *
     * @param topicModel 话题model
     */
    fun resolveTopicResult(topicModel: TopicModel) {
        val topicId = topicModel.topicId
        val topicName = "#" + topicModel.topicName + "#"
        val topic = TopicModel(topicName, topicId)
        resolveTopicText(topic)
    }


    /**
     * 输入了#话题按键的数据返回处理
     *
     * @param topicModel 话题model
     */
    fun resolveTopicResultByEnter(topicModel: TopicModel) {
        val topicId = topicModel.topicId
        deleteByEnter = true
        if (selectionEnd == 0) {
            text.delete(0, 1)
        } else {
            val index = text.toString().indexOf("#", selectionEnd - 1)
            if (index != -1) {
                text.delete(index, index + 1)
            }
        }
        val topicName = "#" + topicModel.topicName + "#"
        val topic = TopicModel(topicName, topicId)
        resolveTopicText(topic)

    }


    /***
     * 按了@按键的数据返回处理
     *
     * @param userModel       用户model
     */
    fun resolveAtResult(userModel: UserModel) {
        val user_id = userModel.user_id
        val user_name = "@" + userModel.user_name!!
        val user = UserModel(user_name, user_id)
        resolveText(user)
    }

    /***
     * 发布的时候输入了AT的返回处理
     *
     * @param userModel       用户model
     */
    fun resolveAtResultByEnterAt(userModel: UserModel) {
        val user_id = userModel.user_id
        if (selectionEnd == 0) {
            text.delete(0, 1)
        } else {
            val index = text.toString().indexOf("@", selectionEnd - 1)
            if (index != -1) {
                text.delete(index, index + 1)
            }
        }
        val user_name = "@" + userModel.user_name!!
        val user = UserModel(user_name, user_id)
        resolveText(user)

    }

    /**
     * 插入表情
     *
     * @param name
     */
    fun insertIcon(name: String) {

        val curString = text.toString()
        if (curString.length + name.length > richMaxLength) {
            return
        }

        val resId = SmileUtils.getRedId(name)

        val drawable = this.resources.getDrawable(resId) ?: return
        drawable.setBounds(0, 0, richIconSize, richIconSize)//这里设置图片的大小
        val imageSpan = ImageSpan(drawable)
        val spannableString = SpannableString(name)
        spannableString.setSpan(imageSpan, 0, spannableString.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)


        val index = Math.max(selectionStart, 0)
        val spannableStringBuilder = SpannableStringBuilder(text)
        spannableStringBuilder.insert(index, spannableString)

        setText(spannableStringBuilder)
        setSelection(index + spannableString.length)


    }

    /**
     * 插入表情文本
     *
     * @param string
     */
    fun insertIconString(string: String) {

        val curString = text.toString()
        if (curString.length + string.length > richMaxLength) {
            return
        }
        val index = Math.max(selectionStart, 0)
        val stringBuilder = StringBuilder(text)
        stringBuilder.insert(index, string)

        setText(stringBuilder)
        setSelection(index + string.length)

    }

    /**
     * 最大长度
     *
     * @param maxLength
     */
    fun setEditTextMaxLength(maxLength: Int) {
        this.richMaxLength = maxLength
    }

    fun getEditTextMaxLength(): Int {
        return richMaxLength
    }
}