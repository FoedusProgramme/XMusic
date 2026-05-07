package com.xapps.media.xmusic.fragment;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.core.view.ViewKt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.entity.Library;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.databinding.*;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import kotlin.Unit;

public class UsedLibsFragment extends BaseFragment {

    private ActivityMainBinding activityBinding;
    private FragmentUsedLibsBinding binding;
	private MainActivity activity;
	
	private ListAdapter adapter;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = FragmentUsedLibsBinding.inflate(inflater, container, false);
        activity = (MainActivity) getActivity();
        init();
        return binding.getRoot();
	}
    
    private void init() {
		ViewKt.doOnLayout(activity.getBinding().bottomNavigation, v -> {
            try {
				Libs libs = new Libs.Builder().withJson(new String(getActivity().getResources().openRawResource(R.raw.aboutlibraries).readAllBytes())).build();
			    List<Library> list = libs.getLibraries();
				adapter = new ListAdapter(getActivity(), list);
				binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
				binding.recyclerView.setAdapter(adapter);
			    binding.recyclerView.addItemDecoration(new SpacingDecoration(activity.getBinding().bottomNavigation.getHeight()));
		    } catch (Exception e) {
			
		    }
            return Unit.INSTANCE;
        });
	}

    public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
		
        int c1 = MaterialColorUtils.colorPrimary;
        int c2 = MaterialColorUtils.colorSecondary;
        int c3 = MaterialColorUtils.colorOnSurface;
        int c4 = MaterialColorUtils.colorOutline;
        
        private List<Library> libsList;
        
        private int spacing;
        
        private static final int TYPE_TOP = 0;
        private static final int TYPE_MIDDLE = 1;
        private static final int TYPE_BOTTOM = 2;
        
        private LibrariesItemLayoutBinding binding;
        
		public ListAdapter(Context c, List<Library> list) {
            spacing = XUtils.convertToPx(c, 5f);
            libsList = list;
        }
		
		@Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int layout;
			
            if (viewType == TYPE_TOP) layout = R.layout.libraries_item_layout_top;
            else if (viewType == TYPE_BOTTOM) layout = R.layout.libraries_item_layout_bottom;
            else layout = R.layout.libraries_item_layout;
			
            return new ViewHolder(inflater.inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
			Library lib = libsList.get(position);
		    View view = holder.itemView;
            binding = LibrariesItemLayoutBinding.bind(view);
			
			String libName = "Unknown";
			
			if (!lib.getName().trim().isEmpty()) libName = lib.getName();
			
			binding.libName.setText(libName);
			binding.libVersion.setText(lib.getArtifactVersion());
			String license = "Unknown";

            if (lib.getLicenses() != null && !lib.getLicenses().isEmpty()) {
                license = lib.getLicenses().iterator().next().getName();
				if (license.trim().isEmpty()) license = "Unknown";
            }		

            binding.licenseChip.setText(license);
			
			String owner = "Unknown";

            if (lib.getDevelopers() != null && !lib.getDevelopers().isEmpty()) {
                owner = lib.getDevelopers().get(0).getName();
            } else if (lib.getOrganization() != null) {
                owner = lib.getOrganization().getName();
            }
			

            if (owner.trim().isEmpty()) {
                owner = "Unknown";
            }

            binding.ownerName.setText(owner);
	    }
        
        @Override
        public int getItemCount() {
            return libsList.size();
        }
        
        @Override
        public int getItemViewType(int position) {
            int size = getItemCount();
    
            if (position == 0) return TYPE_TOP;
            if (position == size - 1) return TYPE_BOTTOM;
            return TYPE_MIDDLE;
        }
		
		static class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}

    public class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int bottomSpacing;
        private int spacing;
        private int sideSpacing;
		private int topSpacing;
        public SpacingDecoration(int bottomSpacing) {
            sideSpacing = XUtils.convertToPx(getActivity(), 12f);
			topSpacing = XUtils.convertToPx(getActivity(), 16f);
            this.bottomSpacing = bottomSpacing;
            spacing = XUtils.convertToPx(getActivity(), 2f);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) return;
            if (position == state.getItemCount() -1 ) {
                outRect.set(sideSpacing, 0, sideSpacing, bottomSpacing);
            } else if (position == 0) {
                outRect.set(sideSpacing, topSpacing, sideSpacing, spacing);
            } else {
                outRect.set(sideSpacing, 0, sideSpacing, spacing);
            }
        }
    }
}


