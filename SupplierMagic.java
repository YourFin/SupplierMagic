// Note: This file is not valid java, but all that needs to be done to make it so is splitting the three public classes into their own files
//
// This file was initally written just past midnight in f*king notepad, and is the result of immense frustration with how un-readable modern approaches
// to "testable" java are.
//
// I claim that these two classes can be used to fairly easily cut ~50% of files in most "solid" OO java programs, and make them drastically easier to
// read as a result.
//
// Further, it can do /a lot/ to eliminate the need for mockito et. all, simply by replacing everything with thunks, which java 8 actually has syntax for!
// I would argue that this also helps with readability by reducing the number of words on the screen to those that are actually relevant.
//
// The mechanism of providers also becomes much more obvious at the top level with this.
// If you see "() -> expr", then you know that `expr` is going to be re-calculated every time a value is requested from the supplier.
// If you see "new LazySupplier(() -> expr)", then you know that `expr` is going to be calculated once, when it is first requested.
// If you see "new StrictSupplier(() -> expr)", then you know that `expr` is going to be calculated once, and that time is now.
// 
// Further, you don't even need to write out "StrictSupplier" if you're feeling lazy, as "T val = expr; () -> val" is equivalent.
//
// Thunk (noun). A function with no input. Pun from functional programming, as in "a value that becomes available after it has been thunk"

// StrictSupplier calculates the value in thunk when it is created. Given how easy it is to replicate with `T val = expr; () -> val`, you may be
// asking yourself "why would I ever want to use this stupid thing. It's extra typing." Well, dear reader, the main purpose here is to show /intent/.
// By typing `new StrictSupplier(() -> expr)`, you make it abundantly clear to the reader of your coffee-flavored software that you intend for this
// thing (who are we kidding, you're doing dependency injection with this g**d*** library, it's a dependency) that you mean for this value to be calculated
// in a strict manner. 
public class StrictSupplier<T> implements Supplier<T> {
	private final T thunkVal;
	pubilc StrictSupplier(Suppiler<T> thunk) {
		this.val = thunk.get();
	}
	
	public T get() {
		return thunkVal;
	}
}

// This fucker is like `StrictSupplier`, but now it's a lazy cunt instead, and procrastinates actually thinking through it's thunk until somebody actually wants something
// from it ("hey, did you get() that `BeanFactoryBuilder` that I asked for yet?" you ask. "Uh, totally. Just gimme one sec." says the `LazySupplier<BeanFactoryBuilder>`). 
// This is probably what you should be doing with your expensive things so long as they don't need to happen at program startup.
//
// If you want people to complain about your program taking too long to start, use a `StrictSupplier`. If you want people to complain about your program being unresponsive,
// use a `LazySupplier`. It's that easy!
//
// Please ignore the fact that this class depends on untested (by me for now) volitile closure magic.
// See https://docs.oracle.com/javase/tutorial/essential/concurrency/atomic.html
public class LazySupplier<T> implements Supplier<T> {

	private volatile Supplier<T> wrapperThunk;
	
	public LazySupplier<T>(Supplier<T> thunk) { 
		this.wrapperThunk = new SyncronizedOneShotSupplier(thunk);
	}

	// Note the lack of static on the next line
	private class SyncronizedOneShotSupplier<T> implements Supplier<T> {
		private final Supplier<T> wrappedThunk;
		public SyncronizedOneShotSupplier(Supplier<T> wrappedThunk) {
			this.wrappedThunk = wrappedThunk;
		}

		public synchronized T get() {
			T val = wrappedThunk.get();
			wrapperThunk = new StrictSupplier(() -> val); // <- magic
			return val;
		}
	}
			
			
	public T get() {
		return wrapperThunk.get();
	}
}

// But wait, there's more! If you order now, we'll throw in the `Suppliers` class for free! 
public class Suppliers {
	public static <T> Supplier<T> strict(Supplier<T> thunk) {
		return new StrictSupplier(thunk);
	}

	public static <T> Supplier<T> lazy(Supplier<T> thunk) {
		return new LazySupplier(thunk);
	}
}
// That's right, by ordering now, we'll do the class-as-namespace abuse for you, so you can do `import static` right out of the box!
// Imagine writing statements like "strict(WrechedWebServer::create)" and "lazy(() -> respondToManager())"!
// It's all your's for the low low price of please save me from this horrible hell why am i here why why why
