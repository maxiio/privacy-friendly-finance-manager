package org.secuso.privacyfriendlyfinance.activities.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.secuso.privacyfriendlyfinance.BR;
import org.secuso.privacyfriendlyfinance.R;
import org.secuso.privacyfriendlyfinance.activities.adapter.NullableArrayAdapter;
import org.secuso.privacyfriendlyfinance.activities.helper.CurrencyInputFilter;
import org.secuso.privacyfriendlyfinance.activities.viewmodel.RepeatingTransactionDialogViewModel;
import org.secuso.privacyfriendlyfinance.databinding.DialogRepeatingTransactionBinding;
import org.secuso.privacyfriendlyfinance.domain.model.Account;
import org.secuso.privacyfriendlyfinance.domain.model.Category;
import org.secuso.privacyfriendlyfinance.domain.model.RepeatingTransaction;

import java.util.List;

public class RepeatingTransactionDialog extends AppCompatDialogFragment {
    public static final String EXTRA_CATEGORY_ID = "org.secuso.privacyfriendlyfinance.EXTRA_CATEGORY_ID";
    public static final String EXTRA_ACCOUNT_ID = "org.secuso.privacyfriendlyfinance.EXTRA_ACCOUNT_ID";
    public static final String EXTRA_TRANSACTION_ID = "org.secuso.privacyfriendlyfinance.EXTRA_TRANSACTION_ID";

    private AlertDialog dialog;
    private View view;
    private EditText editTextAmount;
    private TextView editTextDate;
    private Spinner categorySpinner;
    private Spinner accountSpinner;

    private RepeatingTransactionDialogViewModel viewModel;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        viewModel = ViewModelProviders.of(this).get(RepeatingTransactionDialogViewModel.class);

        final DialogRepeatingTransactionBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_repeating_transaction, null, false);
        view = binding.getRoot();
        builder.setView(view);

        editTextAmount = view.findViewById(R.id.dialog_transaction_amount);
        editTextDate = view.findViewById(R.id.dialog_transaction_date);
        categorySpinner = view.findViewById(R.id.category_spinner);
        accountSpinner = view.findViewById(R.id.account_spinner);

        viewModel.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (propertyId == BR.expense) {
                    editTextAmount.setTextColor(getResources().getColor(viewModel.getAmountColor()));
                }
            }
        });

        long transactionId = getArguments().getLong(EXTRA_TRANSACTION_ID, -1L);

        if (transactionId >= 0) {
            builder.setTitle(R.string.dialog_transaction_edit_title);
        } else {
            builder.setTitle(R.string.dialog_transaction_create_title);
        }
        if (viewModel.getTransaction() == null) {
            viewModel.setTransactionId(transactionId).observe(this, new Observer<RepeatingTransaction>() {
                @Override
                public void onChanged(@Nullable RepeatingTransaction transaction) {
                    // Is it a transaction dummy?
                    if (transaction.getId() == null) {
                        Log.d("acc id", getArguments().getLong(EXTRA_ACCOUNT_ID, -1L) + "");
                        transaction.setAccountId(getArguments().getLong(EXTRA_ACCOUNT_ID, -1L));
                        transaction.setCategoryId(getArguments().getLong(EXTRA_CATEGORY_ID, -1L));
                    }
                    viewModel.setTransaction(transaction);
                    binding.setViewModel(viewModel);
                    editTextAmount.setTextColor(getResources().getColor(viewModel.getAmountColor()));
                }
            });
        } else {
            binding.setViewModel(viewModel);
        }

        viewModel.getAllAccounts().observe(this, new Observer<List<Account>>() {
            @Override
            public void onChanged(@Nullable List<Account> accounts) {
                accountSpinner.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item, accounts));
            }
        });

        viewModel.getAllCategories().observe(this, new Observer<List<Category>>() {
            @Override
            public void onChanged(@Nullable List<Category> categories) {
                categorySpinner.setAdapter(new NullableArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item, categories));
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                viewModel.cancel();
            }
        });

        builder.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                viewModel.submit();
            }
        });

        editTextAmount.setFilters(new InputFilter[] {new CurrencyInputFilter()});

        editTextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePickerTransactionDate();
            }
        });


        dialog = builder.create();
        return dialog;
    }


    private void openDatePickerTransactionDate() {
        openDatePicker(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                viewModel.setEnd(new LocalDate(year, month + 1, dayOfMonth));
            }

        });
    }

    private void openDatePicker(DatePickerDialog.OnDateSetListener listener) {
        LocalDate date = viewModel.getEnd();
        if (date == null) date = LocalDate.now();
        new DatePickerDialog(getContext(), listener, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth()).show();
        dialog.show();
    }
}