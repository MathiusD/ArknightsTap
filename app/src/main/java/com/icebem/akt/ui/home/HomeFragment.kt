package com.icebem.akt.ui.home

import android.content.Intent
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.snackbar.Snackbar
import com.icebem.akt.R
import com.icebem.akt.activity.AboutActivity
import com.icebem.akt.activity.MainActivity
import com.icebem.akt.app.PreferenceManager
import com.icebem.akt.app.ResolutionConfig
import com.icebem.akt.util.AppUtil
import com.icebem.akt.util.DataUtil
import com.icebem.akt.util.IOUtil
import com.icebem.akt.util.RandomUtil
import org.json.JSONObject
import java.io.IOException

class HomeFragment : Fragment() {
    private var i = 0
    private lateinit var state: TextView
    private lateinit var stateImg: ImageView
    private lateinit var manager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val tip = root.findViewById<TextView>(R.id.txt_tips)
        stateImg = root.findViewById(R.id.img_state)
        state = root.findViewById(R.id.txt_state)
        manager = PreferenceManager.getInstance(requireContext())
        if (manager.isPro && !manager.isActivated) {
            tip.setText(R.string.tip_not_found)
            tip.setOnClickListener {
                manager.isPro = true
                AppUtil.showAlertDialog(requireContext(), getString(R.string.reboot_device), getString(R.string.version_type_changed))
            }
        } else {
            try {
                val array = DataUtil.getSloganData(requireContext())
                val obj = array.getJSONObject(RandomUtil.randomIndex(array.length()))
                tip.isSingleLine = true
                tip.text = getString(R.string.operator_slogan, obj.getString("slogan"), obj.getString("name"))
                tip.postDelayed({
                    tip.ellipsize = TextUtils.TruncateAt.MARQUEE
                    tip.marqueeRepeatLimit = -1
                    tip.isSelected = true
                    tip.isFocusable = true
                    tip.isFocusableInTouchMode = true
                }, 2000)
            } catch (e: Exception) {
                tip.setText(R.string.error_occurred)
            }
        }
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (manager.autoUpdate()) startUpdateThread() else onStateEnd()
    }

    private fun onStateEnd() {
        if (manager.isPro && manager.unsupportedResolution()) {
            stateImg.setImageResource(R.drawable.ic_state_running)
            state.setText(R.string.state_resolution_unsupported)
            val res = ResolutionConfig.getAbsoluteResolution(requireContext())
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.state_resolution_unsupported)
            builder.setMessage(getString(R.string.msg_resolution_unsupported, res[0], res[1]))
            builder.setPositiveButton(R.string.got_it, null)
            builder.setNeutralButton(R.string.action_update) { _, _ -> startUpdateThread() }
            builder.create().show()
        } else {
            stateImg.setImageResource(R.drawable.ic_state_running_anim)
            state.setText(R.string.state_loading)
            AnimatedVectorDrawableCompat.registerAnimationCallback(stateImg.drawable, object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    stateImg.setImageResource(R.drawable.ic_state_ready_anim)
                    state.setText(R.string.state_ready)
                    (stateImg.drawable as Animatable).start()
                    if (!stateImg.isClickable) stateImg.setOnClickListener {
                        if (++i >= 3 && i < 15) {
                            if (i == 3) {
                                stateImg.setImageResource(R.drawable.ic_state_error_anim)
                                state.setText(R.string.error_occurred)
                            }
                            (stateImg.drawable as Animatable).start()
                        } else if (i >= 3) {
                            i = 0
                            onAnimationEnd(stateImg.drawable)
                        }
                    }
                }
            })
            (stateImg.drawable as Animatable).start()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_timer -> {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(R.string.action_timer)
                builder.setSingleChoiceItems(manager.getTimerStrings(requireContext()), manager.timerPosition) { dialog, which ->
                    dialog.cancel()
                    manager.timerTime = which
                    Snackbar.make(state, getString(R.string.info_timer_set, if (manager.timerTime == 0) getString(R.string.info_timer_none) else getString(R.string.info_timer_min, manager.timerTime)), Snackbar.LENGTH_LONG).show()
                }
                builder.setNegativeButton(android.R.string.cancel, null)
                builder.create().show()
            }
            R.id.action_night -> AppCompatDelegate.setDefaultNightMode(if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_YES)
            R.id.action_about -> startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startUpdateThread() {
        Thread({ checkVersionUpdate() }, AppUtil.THREAD_UPDATE).start()
        Snackbar.make(state, R.string.version_checking, Snackbar.LENGTH_LONG).show()
    }

    private fun checkVersionUpdate() {
        var id = R.string.version_update
        var log: String? = null
        var url: String? = null
        try {
            if (AppUtil.isLatestVersion) {
                id = if (DataUtil.updateData(manager, true)) R.string.data_updated else R.string.version_latest
            } else {
                val json = JSONObject(IOUtil.stream2String(IOUtil.fromWeb(AppUtil.URL_RELEASE_LATEST_API)))
                log = AppUtil.getChangelog(json)
                url = AppUtil.getDownloadUrl(json)
            }
            manager.setCheckLastTime(false)
        } catch (e: Exception) {
            id = R.string.version_checking_failed
            if (e is IOException) log = getString(R.string.msg_network_error)
        }
        val fab = (requireContext() as MainActivity).fab
        fab.post {
            if (id != R.string.version_checking_failed) (requireContext() as MainActivity).updateSubtitleTime()
            when {
                id == R.string.version_update -> {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle(id)
                    builder.setMessage(log)
                    builder.setPositiveButton(R.string.action_update) { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    builder.setNegativeButton(R.string.no_thanks, null)
                    builder.create().show()
                }
                log != null -> Snackbar.make(fab, id, Snackbar.LENGTH_INDEFINITE).setAction(R.string.action_details) { AppUtil.showLogDialog(requireContext(), log) }.show()
                else -> Snackbar.make(fab, id, Snackbar.LENGTH_LONG).show()
            }
            onStateEnd()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_home, menu)
        if (manager.isPro) menu.findItem(R.id.action_timer).isVisible = true
    }
}