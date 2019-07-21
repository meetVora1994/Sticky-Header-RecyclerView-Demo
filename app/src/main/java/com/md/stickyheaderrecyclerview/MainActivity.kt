package com.md.stickyheaderrecyclerview

import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val list = ArrayList<String>()
        for (i in 0..199) {
            list.add(Random().nextInt(150).toString())
        }
        val adapter = RvAdapter(list)
        recyclerView.addItemDecoration(StickHeaderItemDecoration(adapter))
        recyclerView.setAdapter(adapter)
    }

    class StickHeaderItemDecoration(private val mListener: StickyHeaderInterface) : RecyclerView.ItemDecoration() {
        private var mStickyHeaderHeight: Int = 0

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDrawOver(c, parent, state)
            val topChild = parent.getChildAt(0) ?: return

            val topChildPosition = parent.getChildAdapterPosition(topChild)
            if (topChildPosition == RecyclerView.NO_POSITION) {
                return
            }

            val headerPos = mListener.getHeaderPositionForItem(topChildPosition)
            val currentHeader = getHeaderViewForItem(headerPos, parent)
            fixLayoutSize(parent, currentHeader)
            val contactPoint = currentHeader.bottom
            val childInContact = getChildInContact(parent, contactPoint, headerPos)

            if (childInContact != null && mListener.isHeader(parent.getChildAdapterPosition(childInContact))) {
                moveHeader(c, currentHeader, childInContact)
                return
            }

            drawHeader(c, currentHeader)
        }

        private fun getHeaderViewForItem(headerPosition: Int, parent: RecyclerView): View {
            val layoutResId = mListener.getHeaderLayout(headerPosition)
            val header = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false)
            mListener.bindHeaderData(header, headerPosition)
            return header
        }

        private fun drawHeader(c: Canvas, header: View) {
            c.save()
            c.translate(0f, 0f)
            header.draw(c)
            c.restore()
        }

        private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View) {
            c.save()
            c.translate(0f, (nextHeader.top - currentHeader.height).toFloat())
            currentHeader.draw(c)
            c.restore()
        }

        private fun getChildInContact(parent: RecyclerView, contactPoint: Int, currentHeaderPos: Int): View? {
            var childInContact: View? = null
            for (i in 0 until parent.getChildCount()) {
                var heightTolerance = 0
                val child = parent.getChildAt(i)

                //measure height tolerance with child if child is another header
                if (currentHeaderPos != i) {
                    val isChildHeader = mListener.isHeader(parent.getChildAdapterPosition(child))
                    if (isChildHeader) {
                        heightTolerance = mStickyHeaderHeight - child.getHeight()
                    }
                }

                //add heightTolerance if child top be in display area
                val childBottomPosition: Int
                if (child.getTop() > 0) {
                    childBottomPosition = child.getBottom() + heightTolerance
                } else {
                    childBottomPosition = child.getBottom()
                }

                if (childBottomPosition > contactPoint) {
                    if (child.getTop() <= contactPoint) {
                        // This child overlaps the contactPoint
                        childInContact = child
                        break
                    }
                }
            }
            return childInContact
        }

        /**
         * Properly measures and layouts the top sticky header.
         *
         * @param parent ViewGroup: RecyclerView in this case.
         */
        private fun fixLayoutSize(parent: ViewGroup, view: View) {

            // Specs for parent (RecyclerView)
            val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

            // Specs for children (headers)
            val childWidthSpec = ViewGroup.getChildMeasureSpec(
                widthSpec,
                parent.paddingLeft + parent.paddingRight,
                view.layoutParams.width
            )
            val childHeightSpec = ViewGroup.getChildMeasureSpec(
                heightSpec,
                parent.paddingTop + parent.paddingBottom,
                view.layoutParams.height
            )

            view.measure(childWidthSpec, childHeightSpec)

            mStickyHeaderHeight = view.measuredHeight
            view.layout(0, 0, view.measuredWidth, mStickyHeaderHeight)
        }

        interface StickyHeaderInterface {

            /**
             * This method gets called by [StickHeaderItemDecoration] to fetch the position of the header item in the adapter
             * that is used for (represents) item at specified position.
             *
             * @param itemPosition int. Adapter's position of the item for which to do the search of the position of the header item.
             * @return int. Position of the header item in the adapter.
             */
            fun getHeaderPositionForItem(itemPosition: Int): Int

            /**
             * This method gets called by [StickHeaderItemDecoration] to get layout resource id for the header item at specified adapter's position.
             *
             * @param headerPosition int. Position of the header item in the adapter.
             * @return int. Layout resource id.
             */
            fun getHeaderLayout(headerPosition: Int): Int

            /**
             * This method gets called by [StickHeaderItemDecoration] to setup the header View.
             *
             * @param header         View. Header to set the data on.
             * @param headerPosition int. Position of the header item in the adapter.
             */
            fun bindHeaderData(header: View, headerPosition: Int)

            /**
             * This method gets called by [StickHeaderItemDecoration] to verify whether the item represents a header.
             *
             * @param itemPosition int.
             * @return true, if item at the specified adapter's position represents a header.
             */
            fun isHeader(itemPosition: Int): Boolean
        }
    }

    class RvAdapter(private val list: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        StickHeaderItemDecoration.StickyHeaderInterface {

        private val HEADER = 1
        private val CONTENT = 2

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                HEADER -> return HeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.list_row_header,
                        parent,
                        false
                    )
                )
                else -> return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_row, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = list[position]
            if (holder is HeaderViewHolder) {
                holder.tv.text = item
            } else {
                (holder as ViewHolder).tv.text = item
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position % 4 == 0) HEADER else CONTENT
        }

        override fun getHeaderPositionForItem(itemPosition: Int): Int {
            var itemPosition = itemPosition
            var headerPosition = 0
            do {
                if (this.isHeader(itemPosition)) {
                    headerPosition = itemPosition
                    break
                }
                itemPosition = itemPosition - 1
            } while (itemPosition >= 0)
            return headerPosition
        }

        override fun getHeaderLayout(headerPosition: Int): Int {
            return R.layout.list_row_header
        }

        override fun bindHeaderData(header: View, headerPosition: Int) {
            (header.findViewById(R.id.tv) as TextView).text = list[headerPosition]
        }

        override fun isHeader(itemPosition: Int): Boolean {
            return itemPosition % 4 == 0
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var tv: TextView

            init {
                tv = itemView.findViewById(R.id.tv)
            }

        }

        inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var tv: TextView

            init {
                tv = itemView.findViewById(R.id.tv)
            }

        }
    }
}
