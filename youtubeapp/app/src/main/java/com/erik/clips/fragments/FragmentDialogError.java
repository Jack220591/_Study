package com.erik.clips.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Design and developed by pongodev.com
 *
 * FragmentDialogError is created to display the error dialog generated in showErrorDialog.
 * Created using DialogFragment.
 * FragmentDialogError создается, чтобы отобразить диалоговое окно ошибки, формируемого в showErrorDialog.
 Созданный с использованием DialogFragment.
 */
public class FragmentDialogError extends DialogFragment {
    // Global field to contain the error dialog
    // Глобальное поле содержит диалоговое окно ошибки
    private Dialog mDialog;

    /**
     * Default constructor. Sets the dialog field to null
     * Конструктор по умолчанию. Устанавливает в диалоговое поле нулевое значение
     */
    public FragmentDialogError() {
        super();
        mDialog = null;
    }

    /**
     * Set the dialog to display
     * Установить диалог для отображения
     *
     * @param dialog An error dialog
     */
    public void setDialog(Dialog dialog) {
        mDialog = dialog;
    }

    /*
     * This method must return a Dialog to the DialogFragment.
     * Этот метод должен возвращать диалог с DialogFragment.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mDialog;
    }
}
