package fun.rich.utils.client.managers.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import fun.rich.utils.client.managers.file.impl.account.Account;
import fun.rich.utils.client.managers.file.impl.account.AccountRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.rich.utils.client.managers.file.ClientFile;
import fun.rich.utils.client.managers.file.exception.FileLoadException;
import fun.rich.utils.client.managers.file.exception.FileSaveException;

import java.io.*;
import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountFile extends ClientFile {
    AccountRepository accountRepository;

    public AccountFile(AccountRepository accountRepository) {
        super("accounts");
        this.accountRepository = accountRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path, getName() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(accountRepository.accountList, writer);
        } catch (JsonIOException | IOException e) {
            throw new FileSaveException("Failed to save accounts to file", e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        Gson gson = new Gson();
        File file = new File(path, getName() + ".json");
        try (FileReader reader = new FileReader(file)) {
            Account[] accounts = gson.fromJson(reader, Account[].class);
            accountRepository.accountList.clear();
            accountRepository.accountList.addAll(Arrays.asList(accounts));
        } catch (IOException e) {
            throw new FileLoadException("Failed to load accounts from file", e);
        } catch (JsonSyntaxException e) {
            throw new FileLoadException("JSON syntax error, accounts config cannot be loaded", e);
        } catch (JsonIOException e) {
            throw new FileLoadException("JSON IO error, accounts config cannot be loaded", e);
        }
    }
}