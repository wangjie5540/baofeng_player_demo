package bf.cloud.android.utils;

import java.lang.reflect.Field;

import android.content.Context;

/**
 * 读取资源文件
 */
public class BFYResUtil {

    public static int getLayoutId(Context context, String name) {
        return context.getResources().getIdentifier(name, "layout", context.getPackageName());
    }

    public static int getStringId(Context context, String name) {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }

    public static int getDrawableId(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    public static int getStyleId(Context context, String name) {
        return context.getResources().getIdentifier(name, "style", context.getPackageName());
    }

    public static int getId(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

    public static int getColorId(Context context, String name) {
        return context.getResources().getIdentifier(name, "color", context.getPackageName());
    }

    public static int getAnimId(Context context, String name) {
        return context.getResources().getIdentifier(name, "anim", context.getPackageName());
    }

    public static int getDimenId(Context context, String name) {
        return context.getResources().getIdentifier(name, "dimen", context.getPackageName());
    }
    
    public static int getInt(Context context, String className, String fieldName)
    {
        try {
            Field[] fields = Class.forName(context.getPackageName() + ".R$" + className).getFields();

            for (Field f: fields) {
                if (f.getName().equals(fieldName)) {
                    int result = (Integer)f.get(null);
                    return result;
                }
            }
        } catch (Throwable t){
        }

        return 0;
    }

    public static int[] getIntArray(Context context, String className, String fieldName)
    {
        try {
            Field[] fields = Class.forName(context.getPackageName() + ".R$" + className).getFields();

            for (Field f: fields) {
                if (f.getName().equals(fieldName)) {
                    int[] ret = (int[])f.get(null);
                    return ret;
                }
            }
        } catch (Throwable t){
        }

        return null;
    }
    
}
