package betterbundle.gui;

public interface BundleRecipeBookAccess {
    /** The active bundle page, or null while the book is closed or displaying recipes. */
    BundleRecipeBookPage betterbundle$getPage();
}
