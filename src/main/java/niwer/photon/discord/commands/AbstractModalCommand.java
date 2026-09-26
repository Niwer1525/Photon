package niwer.photon.discord.commands;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

public abstract class AbstractModalCommand extends AbstractSlashCommand {

    private final Modal MODAL;

    protected AbstractModalCommand(String cmdName, String cmdDescription, String modalTitle, TextInputField... fields) {
        super(cmdName, cmdDescription);
        this.MODAL = Modal.create(cmdName + "_modal", modalTitle).addComponents(buildModalComponents(fields)).build();
    }
    
    private List<Label> buildModalComponents(TextInputField... fields) {
        if (fields == null || fields.length == 0) throw new IllegalArgumentException("At least one TextInputField must be provided.");

        final List<Label> COMPONENTS = new ArrayList<>();
        for (TextInputField field : fields) {
            final var LABEL = Label.of(field.label, field.asTextInput());
            COMPONENTS.add(LABEL);
        }
        return COMPONENTS;
    }

    public final Modal modal() { return this.MODAL; }

    public final String modalId() { return this.MODAL.getId(); }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.replyModal(this.MODAL).queue(); // By default, when the slash command is invoked, we reply with the modal.
    }

    public abstract void handleForm(ModalInteractionEvent event);

    protected static class TextInputField {
        private final String id;
        private final String label;
        private final boolean required;

        private TextInputStyle style = TextInputStyle.SHORT;
        private String placeholder = null;
        private int minLength = -1;
        private int maxLength = -1;

        public TextInputField(String id, String label, boolean required) {
            this.id = id;
            this.label = label;
            this.required = required;
        }

        public TextInputField setStyle(TextInputStyle style) {
            this.style = style;
            return this;
        }

        public TextInputField setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public TextInputField setMinLength(int minLength) {
            this.minLength = minLength;
            return this;
        }

        public TextInputField setMaxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        private TextInput asTextInput() {
            final TextInput INPUT = TextInput.create(this.id, this.style)
                    .setRequired(this.required)
                    .setPlaceholder(this.placeholder)
                    .setMinLength(this.minLength)
                    .setMaxLength(this.maxLength)
                    .build();
            return INPUT;
        }
    }
}
