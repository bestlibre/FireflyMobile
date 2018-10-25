package xyz.hisname.fireflyiii.ui.bills

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fontawesome_typeface_library.FontAwesome
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.activity_bill_detail.*
import kotlinx.android.synthetic.main.progress_overlay.*
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.viewmodel.BillsViewModel
import xyz.hisname.fireflyiii.ui.ProgressBar
import xyz.hisname.fireflyiii.ui.base.BaseActivity
import xyz.hisname.fireflyiii.util.extension.getViewModel
import xyz.hisname.fireflyiii.util.extension.toastError
import xyz.hisname.fireflyiii.util.extension.toastSuccess

class BillDetailActivity: BaseActivity(){

    private var billList: MutableList<BillDetailData> = ArrayList()
    private val billVM by lazy { getViewModel(BillsViewModel::class.java) }
    private val billResponse by lazy { billVM.getBillById(intent.getLongExtra("billId", 0), baseUrl, accessToken) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill_detail)
        setSupportActionBar(toolbar)
        runLayoutAnimation(recycler_view)
        toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_left)
        toolbar.setNavigationOnClickListener { finish() }
        showData()
        editBill()
    }


    private fun showData(){
        billResponse.databaseData?.observe(this, Observer {
            if(it.size == 1) {
                val billAttribute = it[0].billAttributes
                billName.text = billAttribute?.name
                val billDataArray = arrayListOf(
                        BillDetailData("Updated At", billAttribute?.updated_at,
                                IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_update).sizeDp(24)),
                        BillDetailData("Created At", billAttribute?.created_at,
                                IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_create).sizeDp(24)),
                        BillDetailData("Currency Code", billAttribute?.currency_code,
                                IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_local_atm).sizeDp(24)),
                        BillDetailData("Currency ID", billAttribute?.currency_id.toString(),
                                IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_local_atm).sizeDp(24)),
                        BillDetailData("Amount Min", billAttribute?.amount_min.toString(),
                                IconicsDrawable(this).icon(FontAwesome.Icon.faw_minus).sizeDp(24)),
                        BillDetailData("Amount Max", billAttribute?.amount_max.toString(),
                                IconicsDrawable(this).icon(FontAwesome.Icon.faw_plus).sizeDp(24)),
                        BillDetailData("Date", billAttribute?.date,
                                ContextCompat.getDrawable(this, R.drawable.ic_calendar_blank)),
                        BillDetailData("Repeat Frequency", billAttribute?.repeat_freq,
                                IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_repeat).sizeDp(24)),
                        BillDetailData("Skip", billAttribute?.skip.toString(),
                                IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_skip_next).sizeDp(24)),
                        BillDetailData("Automatch", billAttribute?.automatch.toString(),
                                IconicsDrawable(this).icon(FontAwesome.Icon.faw_magic).sizeDp(24)),
                        if (billAttribute?.pay_dates!!.isEmpty()) {
                            BillDetailData("Pay Dates", "No dates found",
                                    IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_credit_card).sizeDp(24))
                        } else {
                            BillDetailData("Pay Dates", billAttribute.pay_dates.toString(),
                                    IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_credit_card).sizeDp(24))
                        },
                        if (billAttribute.paid_dates.isEmpty()) {
                            BillDetailData("Paid Dates", "No dates found",
                                    IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_credit_card).sizeDp(24))
                        } else {
                            BillDetailData("Paid Dates", billAttribute.pay_dates.toString(),
                                    IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_credit_card).sizeDp(24))
                        }
                )
                billList.addAll(billDataArray)
            }
            recycler_view.adapter?.notifyDataSetChanged()
        })
        recycler_view.adapter = BillDetailRecyclerAdapter(billList)
        billResponse.apiResponse.observe(this, Observer {
            if(it.getError() != null){
                toastError(it.getError()?.message)
            }
        })
    }

    private fun editBill(){
        editBillFab.setOnClickListener {
            // TODO: Add update logic here
            val billDetail = Intent(this, AddBillActivity::class.java).apply {
                putExtras(bundleOf("billId" to intent.getLongExtra("billId", 0)))
            }
            startActivity(billDetail)
        }

    }

    private fun runLayoutAnimation(recyclerView: RecyclerView){
        val controller = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BillDetailActivity)
            layoutAnimation = controller
            adapter?.notifyDataSetChanged()
            scheduleLayoutAnimation()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.delete_menu, menu)
        return true
    }

    private fun deleteItem(){
        ProgressBar.animateView(progress_overlay, View.VISIBLE, 0.4f, 200)
        billVM.deleteBill(baseUrl, accessToken, intent.getLongExtra("billId", 0).toString()).observe(this, Observer {
            if (it.getError() != null) {
                ProgressBar.animateView(progress_overlay, View.GONE, 0f, 200)
                val parentLayout: View = findViewById(R.id.coordinatorlayout)
                Snackbar.make(parentLayout, R.string.generic_delete_error, Snackbar.LENGTH_LONG)
                        .setAction("Retry") { _ ->
                            deleteItem()
                        }
                        .show()
            } else {
                toastSuccess(resources.getString(R.string.bill_deleted))
                finish()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(item?.itemId == R.id.menu_item_delete) {
            AlertDialog.Builder(this)
                    .setTitle("Are you sure?")
                    .setMessage(R.string.irreversible_action)
                    .setPositiveButton("Yes") { _, _ ->
                        deleteItem()
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .show()
        }
        return true
    }

}