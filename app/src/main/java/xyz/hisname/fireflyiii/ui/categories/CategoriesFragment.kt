package xyz.hisname.fireflyiii.ui.categories

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.base_swipe_layout.*
import kotlinx.android.synthetic.main.fragment_base_list.*
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.models.category.CategoryData
import xyz.hisname.fireflyiii.ui.base.BaseFragment
import xyz.hisname.fireflyiii.util.extension.create
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.android.synthetic.main.activity_base.*
import xyz.hisname.fireflyiii.util.EndlessRecyclerViewScrollListener
import xyz.hisname.fireflyiii.util.extension.display
import xyz.hisname.fireflyiii.util.extension.hideFab


class CategoriesFragment: BaseFragment() {

    private lateinit var scrollListener: EndlessRecyclerViewScrollListener
    private val catData by lazy { arrayListOf<CategoryData>() }
    private val catAdapter by lazy { CategoriesRecyclerAdapter(catData) { data: CategoryData -> }  }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_base_list, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayView()
        initFab()
        pullToRefresh()
        recycler_view.layoutManager = linearLayout()
        recycler_view.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recycler_view.adapter = catAdapter
    }

    private fun displayView(){
        swipeContainer.isRefreshing = true
        categoryViewModel.getPaginatedCategory(1).observe(this){ categoryList ->
            setList(categoryList)
        }
        scrollListener = object : EndlessRecyclerViewScrollListener(linearLayout()){
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                // Don't load more when data is refreshing
                if(!swipeContainer.isRefreshing) {
                    swipeContainer.isRefreshing = true
                    categoryViewModel.getPaginatedCategory(page + 1).observe(this@CategoriesFragment) { catList ->
                        catData.clear()
                        catData.addAll(catList)
                        catAdapter.update(catData)
                        catAdapter.notifyDataSetChanged()
                        swipeContainer.isRefreshing = false
                    }
                }
            }
        }
        recycler_view.addOnScrollListener(scrollListener)
    }

    private fun setList(categoryData: MutableList<CategoryData>){
        swipeContainer.isRefreshing = false
        if (categoryData.isNotEmpty()) {
            listText.isVisible = false
            listImage.isVisible = false
            recycler_view.isVisible = true
            catData.addAll(categoryData)
            catAdapter.update(catData)
            catAdapter.notifyDataSetChanged()
        } else {
            listText.text = "No category found"
            listText.isVisible = true
            listImage.isVisible = true
            listImage.setImageDrawable(IconicsDrawable(requireContext())
                    .icon(FontAwesome.Icon.faw_chart_bar)
                    .sizeDp(24))
            recycler_view.isVisible = false
        }
    }

    private fun initFab(){
        fab.display {
            fab.isClickable = false
            val addCategoryFragment = AddCategoriesFragment()
            addCategoryFragment.show(parentFragmentManager, "add_category_fragment")
            fab.isClickable = true
        }
        recycler_view.hideFab(fab)
    }

    private fun pullToRefresh(){
        swipeContainer.setOnRefreshListener {
            scrollListener.resetState()
            catData.clear()
            catAdapter.notifyDataSetChanged()
            displayView()
        }
    }


    override fun onAttach(context: Context){
        super.onAttach(context)
        activity?.activity_toolbar?.title = resources.getString(R.string.categories)
    }

    override fun onResume() {
        super.onResume()
        activity?.activity_toolbar?.title = resources.getString(R.string.categories)
    }

    override fun handleBack() {
        parentFragmentManager.popBackStack()
    }
}