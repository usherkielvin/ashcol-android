package app.hub.user;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.servicehub.adapter.ChatAdapter;
import com.servicehub.model.ChatRequest;
import com.servicehub.model.ChatResponse;
import com.servicehub.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import app.hub.R;
import app.hub.util.TokenManager;

/**
 * Bottom sheet fragment for the chatbot interface.
 * Provides a fullscreen, immersive chat experience with drag-to-dismiss functionality.
 */
public class ChatbotBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "ChatbotBottomSheet";

    private RecyclerView recyclerView;
    private TextInputEditText messageEditText;
    private MaterialButton sendButton;
    private View loadingIndicator;
    private MaterialButton minimizeButton;
    
    private List<Message> messageList;
    private ChatAdapter chatAdapter;
    
    private TokenManager tokenManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set custom theme for rounded corners and styling
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
        
        // Initialize token manager
        tokenManager = new TokenManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        // Inflate the chatbot bottom sheet layout
        return inflater.inflate(R.layout.chatbot_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components
        initializeViews(view);
        setupRecyclerView();
        setupInputField();
        setupSendButton();
        setupMinimizeButton();
    }

    /**
     * Initialize all view references.
     */
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        messageEditText = view.findViewById(R.id.messageEditText);
        sendButton = view.findViewById(R.id.sendButton);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        minimizeButton = view.findViewById(R.id.minimizeButton);
    }

    /**
     * Set up the RecyclerView with LinearLayoutManager and ChatAdapter.
     */
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(chatAdapter);
        
        // Restore message history
        restoreMessageHistory();
    }

    /**
     * Restore message history from ChatbotStateManager or add welcome message.
     */
    private void restoreMessageHistory() {
        app.hub.util.ChatbotStateManager stateManager = app.hub.util.ChatbotStateManager.getInstance();
        
        if (stateManager.hasHistory()) {
            // Restore existing conversation
            messageList.addAll(stateManager.getMessageHistory());
            chatAdapter.notifyDataSetChanged();
            
            // Scroll to the last message
            if (!messageList.isEmpty()) {
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        } else {
            // Add welcome message for new conversation
            Message welcomeMessage = new Message("Hello! How can I help you today?", false);
            messageList.add(welcomeMessage);
            stateManager.addMessage(welcomeMessage);
            chatAdapter.notifyItemInserted(0);
        }
        
        // Set focus to input field
        if (messageEditText != null) {
            messageEditText.requestFocus();
        }
    }

    /**
     * Set up the input field with TextWatcher to enable/disable send button.
     */
    private void setupInputField() {
        messageEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Enable send button only if there's non-whitespace content
                String text = s.toString().trim();
                sendButton.setEnabled(!text.isEmpty() && !text.matches("\\s+"));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Not needed
            }
        });
        
        // Initially disable send button if input is empty
        sendButton.setEnabled(false);
    }

    /**
     * Set up the send button click listener.
     */
    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            if (messageEditText.getText() != null) {
                String messageText = messageEditText.getText().toString().trim();
                // Prevent empty or whitespace-only messages
                if (!messageText.isEmpty() && !messageText.matches("\\s+")) {
                    sendMessage(messageText);
                    messageEditText.setText("");
                }
            }
        });
    }

    /**
     * Send a message to the chatbot API.
     */
    private void sendMessage(String messageText) {
        // Add user message to chat
        Message userMessage = new Message(messageText, true);
        messageList.add(userMessage);
        app.hub.util.ChatbotStateManager.getInstance().addMessage(userMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        // Add typing indicator
        Message typingIndicator = new Message("", false, true);
        messageList.add(typingIndicator);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        // Disable send button
        sendButton.setEnabled(false);

        // Mock a delayed response from the chatbot
        new android.os.Handler().postDelayed(() -> {
            if (!isAdded()) return;

            // Remove typing indicator
            messageList.remove(messageList.size() - 1);
            chatAdapter.notifyItemRemoved(messageList.size());

            String reply = "I'm currently being migrated to Firebase. For immediate assistance, please contact support@ashcol.com";
            Message botMessage = new Message(reply, false);
            
            messageList.add(botMessage);
            app.hub.util.ChatbotStateManager.getInstance().addMessage(botMessage);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
            
            // Re-enable send button
            sendButton.setEnabled(true);
        }, 1500);
    }

    /**
     * Check if network is available.
     */
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = 
            (android.net.ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;
                
                android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        }
        return false;
    }

    /**
     * Handle API error responses.
     */
    private void handleApiError(Object response) {
        // Not used anymore in Firebase flow
    }

    /**
     * Set up the minimize button to dismiss the bottom sheet.
     */
    private void setupMinimizeButton() {
        minimizeButton.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        
        // Clear input field text
        if (messageEditText != null) {
            messageEditText.setText("");
        }
        
        // Message history is preserved in ChatbotStateManager (don't clear)
    }

    @Override
    public void onStart() {
        super.onStart();
        configureBottomSheetBehavior();
    }

    /**
     * Configure the bottom sheet behavior for fullscreen display with drag-to-dismiss.
     */
    private void configureBottomSheetBehavior() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
            (com.google.android.material.bottomsheet.BottomSheetDialog) getDialog();
        
        if (dialog != null) {
            android.widget.FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
            
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior = 
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                
                // Configure for fullscreen
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setHideable(true);
                behavior.setDraggable(true);
                
                // Set peek height to full screen
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(layoutParams);
                
                // Add callback to hide keyboard when dragging
                behavior.addBottomSheetCallback(new com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        // Hide keyboard when user starts dragging or collapses the sheet
                        if (newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING ||
                            newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING) {
                            hideKeyboard();
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        // Optional: Hide keyboard immediately when slide starts
                        // Uncomment if you want keyboard to hide on first touch
                        // hideKeyboard();
                    }
                });
            }
        }
    }
    
    /**
     * Hide the soft keyboard.
     */
    private void hideKeyboard() {
        if (messageEditText != null) {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) requireContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            
            if (imm != null) {
                imm.hideSoftInputFromWindow(messageEditText.getWindowToken(), 0);
                messageEditText.clearFocus();
            }
        }
    }
}
