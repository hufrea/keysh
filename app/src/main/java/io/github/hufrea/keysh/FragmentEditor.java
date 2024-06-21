package io.github.hufrea.keysh;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import io.github.hufrea.keysh.R;
import io.github.hufrea.keysh.databinding.FragmentEditorBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;


public class FragmentEditor extends Fragment {
    private FragmentEditorBinding binding;
    private FragmentActivity activity;
    String path;
    Context context;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentEditorBinding.inflate(inflater, container, false);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        return binding.getRoot();

    }

    private void createMenu() {
        activity.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.action_exit) {
                    ((ActivityMain)getActivity()).stopBH();
                    return true;
                }
                if (id == R.id.action_save) {
                    writeFile(binding.text.getText().toString());
                    ((ActivityMain)getActivity()).restartBH();
                    return true;
                }
                if (id == R.id.action_settings) {
                    NavHostFragment.findNavController(FragmentEditor.this)
                            .navigate(R.id.action_FirstFragment_to_SecondFragment);
                }
                return false;
            }
        }, this.getViewLifecycleOwner());
    }

    private void initDefaultFile(String default_path) {
        try {
            InputStream is = getActivity().getAssets().open("code.sh");
            FileOutputStream wr = new FileOutputStream(default_path);

            byte[] array = new byte[1024];
            int ws;
            try {
                while ((ws = is.read(array)) != -1) {
                    wr.write(array, 0, ws);
                }
                wr.flush();
            } finally {
                wr.close();
                is.close();
            }
        } catch (IOException e) {
            Log.e("FragmentEditor", e.toString());
        }
    }

    private void writeFile(String text) {
        FileWriter writer;
        try {
            writer = new FileWriter(path);
            try {
                writer.write(text);
                writer.flush();
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            return;
        }
    }

    private String readFile() {
        File file = new File(path);

        if (!file.exists()) {
            return "file not exist";
        }
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            return e.toString();
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getContext();
        if (context == null) {
            return;
        }
        activity = getActivity();
        if (activity == null) {
            return;
        }
        createMenu();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int textsize;
        try {
            textsize = Integer.parseInt(sp.getString("text_size", "14"));
        } catch (NumberFormatException e) {
            textsize = 16;
        }
        String default_path = context.getFilesDir() + "/code.sh";
        String path = sp.getString("path", "");

        if (path.isEmpty()) {
            sp.edit().putString("path", default_path).apply();
            initDefaultFile(default_path);

            this.path = default_path;
        }
        else if (this.path != null && !path.equals(this.path)) {
            getActivity().finish();
            startActivity(getActivity().getIntent());
            ((ActivityMain)getActivity()).restartBH();
        }
        else this.path = path;

        String text = readFile();
        binding.text.setTextSize(textsize);
        binding.text.setText(text);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}