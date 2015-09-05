package tomthomas.nearby;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DrawerItemClickListener implements ListView.OnItemClickListener {

    private MainActivity activity;

    public DrawerItemClickListener(MainActivity activity)
    {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        if(position == 1)
        {
            Intent favIntent = new Intent(parent.getContext(), Favorites.class);
            parent.getContext().startActivity(favIntent);
        }
    }

    public void setTitle(CharSequence title) {
        activity.getSupportActionBar().setTitle(title);
    }
}

