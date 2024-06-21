package io.github.hufrea.keysh.actions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;
import java.util.regex.Pattern;

public class ActionIntent {
    static private Object parseValue(String str) {
        if (str.isEmpty()) {
            return str;
        }
        char c = str.charAt(0);
        if ((c == '"' || c == '\'')
                && str.endsWith(String.valueOf(c))) {
            return str.substring(1, str.length() - 1);
        }
        else if (str.equals("true")) {
            return true;
        }
        else if (str.equals("false")) {
            return false;
        }
        else try {
            c = str.charAt(str.length() - 1);
            switch (c) {
                case 'l':
                case 'L':
                    return Long.parseLong(str.substring(0, str.length() - 1));
                case 'f':
                case 'F':
                    return Float.parseFloat(str.substring(0, str.length() - 1));
                default:
                    if (str.contains("."))
                        return Double.parseDouble(str);
                    else
                        return Integer.parseInt(str);
            }
        } catch (NumberFormatException e) {
            Log.d("ActionIntent", "is not number: " + str);
            return str;
        }
    }

    static private void putIntentExtraArray(String name, String str, Intent intent) {
        str = str.substring(1, str.length() - 1);

        String delimiter = ",";
        if (str.startsWith(":")) {
            String[] split = str.split(":", 3);
            if (split.length < 3) {
                return;
            }
            delimiter = Pattern.quote(split[1]);
            str = split[2];
        }
        String[] array = str.split(delimiter);
        if (array.length == 0) {
            return;
        }
        Object[] objArray = new Object[array.length];

        boolean eqclass = true;
        Object object = parseValue(array[0]);
        Class<?> c = object.getClass();

        for (int i = 0; i < array.length; i++) {
            object = parseValue(array[i]);
            if (eqclass && object.getClass() != c) {
                eqclass = false;
            }
            objArray[i] = object;
        }
        if (!eqclass || c == String.class) {
            intent.putExtra(name,
                    Arrays.copyOf(objArray, objArray.length, String[].class));
        }
        else if (c == Boolean.class) {
            boolean[] extra = new boolean[objArray.length];
            for (int q = 0; q < objArray.length; q++) {
                extra[q] = (boolean) objArray[q];
            }
            intent.putExtra(name, extra);
        }
        else if (c == Integer.class) {
            int[] extra = new int[objArray.length];
            for (int q = 0; q < objArray.length; q++) {
                extra[q] = (int) objArray[q];
            }
            intent.putExtra(name, extra);
        }
        else if (c == Long.class) {
            long[] extra = new long[objArray.length];
            for (int q = 0; q < objArray.length; q++) {
                extra[q] = (long) objArray[q];
            }
            intent.putExtra(name, extra);
        }
        else if (c == Float.class) {
            float[] extra = new float[objArray.length];
            for (int q = 0; q < objArray.length; q++) {
                extra[q] = (float) objArray[q];
            }
            intent.putExtra(name, extra);
        }
        else if (c == Double.class) {
            double[] extra = new double[objArray.length];
            for (int q = 0; q < objArray.length; q++) {
                extra[q] = (double) objArray[q];
            }
            intent.putExtra(name, extra);
        }
    }

    static private void putIntentExtra(String string, Intent intent) {
        if (!string.contains(":")) {
            return;
        }
        String[] a = string.split(":", 2);
        String name = a[0], str = a[1];

        if (str.startsWith("{") && str.endsWith("}")) {
            putIntentExtraArray(name, str, intent);
        }
        else {
            Object object = parseValue(str);

            if (object instanceof Boolean) {
                intent.putExtra(name, (boolean) object);
            } else if (object instanceof Integer) {
                intent.putExtra(name, (int) object);
            } else if (object instanceof Long) {
                intent.putExtra(name, (long) object);
            } else if (object instanceof Float) {
                intent.putExtra(name, (float) object);
            } else if (object instanceof Double) {
                intent.putExtra(name, (double) object);
            } else if (object instanceof String) {
                intent.putExtra(name, (String) object);
            }
        }
    }

    static public void sendIntent(Context context, String[] params) {
        if (params.length < 3) {
            return;
        }
        Intent intent = new Intent();
        String target = "broadcast";

        Uri data = null;
        String type = null;

        for (int i = 1; i < params.length; i++) {
            String arg = params[i], str = "";
            if (arg.startsWith("-")) {
                i++;
                if (i >= params.length) {
                    break;
                }
                str = params[i];
            }
            switch (arg) {
                case "--action":
                case "-a":
                    intent.setAction(str);
                    break;
                case "--package":
                case "-p":
                    if (str.contains("/")) {
                        intent.setComponent(ComponentName.unflattenFromString(str));
                    } else {
                        intent.setPackage(str);
                    }
                    break;
                case "--data":
                case "-d":
                    if (type == null) {
                        data = Uri.parse(str);
                        intent.setData(data);
                    }
                    else
                        intent.setDataAndType(Uri.parse(str), type);
                    break;
                case "--mimetype":
                case "-m":
                    if (data == null) {
                        type = str;
                        intent.setType(type);
                    }
                    else
                        intent.setDataAndType(data, str);
                    break;
                case "--category":
                case "-c":
                    intent.addCategory(str);
                    break;
                case "--extra":
                case "-e":
                    putIntentExtra(str, intent);
                    break;
                case "-f":
                    try {
                        intent.addFlags(Integer.parseInt(str));
                    } catch (NumberFormatException e) {
                    }
                    break;
                case "--target":
                case "-t":
                    target = str;
                    break;
            }
        }
        Log.d("ActionIntent", "start: " + target);
        switch (target) {
            case "activity":
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                break;
            case "broadcast":
                context.sendBroadcast(intent);
                break;
            case "service":
                context.startService(intent);
                break;
        }
    }
}
